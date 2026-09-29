package com.ide.studio.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ide.studio.R;
import com.ide.studio.model.ActionType;
import com.ide.studio.model.AiMessage;
import com.ide.studio.model.ApprovalStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for Build AI Chat.
 * Supports User bubble, Assistant bubble, Plan card, and Action card (with Approve/Reject buttons
 * and persistent Applied status).
 */
public class AiChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_USER = 1;
    public static final int VIEW_TYPE_ASSISTANT_TEXT = 2;
    public static final int VIEW_TYPE_PLAN = 3;
    public static final int VIEW_TYPE_ACTION = 4;

    public interface OnActionCardClickListener {
        void onApprove(AiMessage message, int position);
        void onReject(AiMessage message, int position);
    }

    private final Context mContext;
    private final List<AiMessage> mMessages = new ArrayList<>();
    private final OnActionCardClickListener mListener;

    public AiChatAdapter(Context context, OnActionCardClickListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public void setMessages(List<AiMessage> messages) {
        mMessages.clear();
        if (messages != null) {
            mMessages.addAll(messages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(AiMessage message) {
        mMessages.add(message);
        notifyItemInserted(mMessages.size() - 1);
    }

    public List<AiMessage> getMessages() {
        return mMessages;
    }

    @Override
    public int getItemViewType(int position) {
        AiMessage msg = mMessages.get(position);
        if ("user".equalsIgnoreCase(msg.getRole())) {
            return VIEW_TYPE_USER;
        } else if (msg.getActionType() != null && msg.getActionType() != ActionType.NONE) {
            return VIEW_TYPE_ACTION;
        } else if (msg.getPlanText() != null && !msg.getPlanText().trim().isEmpty()) {
            return VIEW_TYPE_PLAN;
        } else {
            return VIEW_TYPE_ASSISTANT_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        if (viewType == VIEW_TYPE_PLAN) {
            View v = inflater.inflate(R.layout.item_chat_plan, parent, false);
            return new PlanViewHolder(v);
        } else if (viewType == VIEW_TYPE_ACTION) {
            View v = inflater.inflate(R.layout.item_chat_action, parent, false);
            return new ActionViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_chat_message, parent, false);
            return new MessageViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AiMessage message = mMessages.get(position);

        if (holder instanceof MessageViewHolder) {
            MessageViewHolder msgHolder = (MessageViewHolder) holder;
            if ("user".equalsIgnoreCase(message.getRole())) {
                msgHolder.layoutUser.setVisibility(View.VISIBLE);
                msgHolder.layoutAssistant.setVisibility(View.GONE);
                msgHolder.tvUser.setText(message.getContent());
            } else {
                msgHolder.layoutUser.setVisibility(View.GONE);
                msgHolder.layoutAssistant.setVisibility(View.VISIBLE);
                msgHolder.tvAssistant.setText(message.getContent());
            }
        } else if (holder instanceof PlanViewHolder) {
            PlanViewHolder planHolder = (PlanViewHolder) holder;
            planHolder.tvPlan.setText(message.getPlanText() != null ? message.getPlanText() : message.getContent());
        } else if (holder instanceof ActionViewHolder) {
            ActionViewHolder actionHolder = (ActionViewHolder) holder;
            bindActionCard(actionHolder, message, position);
        }
    }

    private void bindActionCard(ActionViewHolder holder, AiMessage message, int position) {
        // Configure Action Badge
        ActionType type = message.getActionType();
        String badgeTitle = "ACTION";
        int badgeColor = Color.parseColor("#00BCD4");

        if (type == ActionType.CREATE_FILE) {
            badgeTitle = "CREATE FILE";
            badgeColor = Color.parseColor("#00BCD4");
        } else if (type == ActionType.WRITE_FILE) {
            badgeTitle = "WRITE FILE";
            badgeColor = Color.parseColor("#5844ED");
        } else if (type == ActionType.RENAME_FILE) {
            badgeTitle = "RENAME FILE";
            badgeColor = Color.parseColor("#F59E0B");
        } else if (type == ActionType.DELETE_FILE) {
            badgeTitle = "DELETE FILE";
            badgeColor = Color.parseColor("#EF4444");
        }

        holder.tvBadge.setText(badgeTitle);
        holder.tvBadge.setTextColor(badgeColor);

        // Target File Path
        String path = message.getTargetFilePath();
        if (path == null || path.isEmpty()) {
            path = message.getNewFilePath();
        }
        holder.tvPath.setText(path != null ? path : "");

        // Proposed Code Snippet
        String snippet = message.getProposedCode();
        if (snippet != null && !snippet.trim().isEmpty()) {
            holder.layoutSnippet.setVisibility(View.VISIBLE);
            holder.tvCode.setText(snippet);
        } else {
            holder.layoutSnippet.setVisibility(View.GONE);
        }

        // Approval Status handling
        ApprovalStatus status = message.getApprovalStatus();
        if (status == ApprovalStatus.APPROVED) {
            // Already approved: do NOT show buttons, show Applied status permanently
            holder.layoutButtons.setVisibility(View.GONE);
            holder.layoutStatus.setVisibility(View.VISIBLE);
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check);
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#10B981"));
            holder.tvStatusText.setText("Applied Successfully");
            holder.tvStatusText.setTextColor(Color.parseColor("#10B981"));
        } else if (status == ApprovalStatus.REJECTED) {
            // Rejected
            holder.layoutButtons.setVisibility(View.GONE);
            holder.layoutStatus.setVisibility(View.VISIBLE);
            holder.ivStatusIcon.setImageResource(R.drawable.ic_close);
            holder.ivStatusIcon.setColorFilter(Color.parseColor("#EF4444"));
            holder.tvStatusText.setText("Changes Rejected");
            holder.tvStatusText.setTextColor(Color.parseColor("#EF4444"));
        } else {
            // PENDING: show Approve and Reject buttons
            holder.layoutButtons.setVisibility(View.VISIBLE);
            holder.layoutStatus.setVisibility(View.GONE);

            holder.btnApprove.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onApprove(message, position);
                }
            });

            holder.btnReject.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onReject(message, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutUser;
        LinearLayout layoutAssistant;
        TextView tvUser;
        TextView tvAssistant;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutUser = itemView.findViewById(R.id.layout_user_message);
            layoutAssistant = itemView.findViewById(R.id.layout_assistant_message);
            tvUser = itemView.findViewById(R.id.tv_user_text);
            tvAssistant = itemView.findViewById(R.id.tv_assistant_text);
        }
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlan;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlan = itemView.findViewById(R.id.tv_plan_content);
        }
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge;
        TextView tvPath;
        LinearLayout layoutSnippet;
        TextView tvCode;
        LinearLayout layoutStatus;
        ImageView ivStatusIcon;
        TextView tvStatusText;
        LinearLayout layoutButtons;
        Button btnApprove;
        Button btnReject;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_action_badge);
            tvPath = itemView.findViewById(R.id.tv_file_path);
            layoutSnippet = itemView.findViewById(R.id.layout_snippet_container);
            tvCode = itemView.findViewById(R.id.tv_code_content);
            layoutStatus = itemView.findViewById(R.id.layout_status);
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
            tvStatusText = itemView.findViewById(R.id.tv_status_text);
            layoutButtons = itemView.findViewById(R.id.layout_buttons);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
