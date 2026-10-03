package com.idestudio.app.ui.ai;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.idestudio.app.R;
import com.idestudio.app.ai.approval.AIProposal;
import com.idestudio.app.ai.approval.ApprovalManager;
import com.idestudio.app.data.models.ChatMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    public interface ProposalApprovalListener {
        void onProposalApproved(AIProposal proposal);
        void onProposalRejected(AIProposal proposal);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    private final File projectRoot;
    private final ProposalApprovalListener listener;

    public ChatMessageAdapter() {
        this(null, null);
    }

    public ChatMessageAdapter(List<ChatMessage> list) {
        this(null, null);
        if (list != null) {
            messages.addAll(list);
        }
    }

    public ChatMessageAdapter(File projectRoot, ProposalApprovalListener listener) {
        this.projectRoot = projectRoot;
        this.listener = listener;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setMessages(List<ChatMessage> list) {
        messages.clear();
        if (list != null) {
            messages.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.bind(msg);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final View layoutUser;
        private final TextView tvUserMessage;
        private final View layoutAi;
        private final TextView tvAiMessage;
        private final LinearLayout containerProposals;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutUser = itemView.findViewById(R.id.layout_user_bubble);
            tvUserMessage = itemView.findViewById(R.id.tv_user_message);
            layoutAi = itemView.findViewById(R.id.layout_ai_bubble);
            tvAiMessage = itemView.findViewById(R.id.tv_ai_message);
            containerProposals = itemView.findViewById(R.id.container_proposals);
        }

        void bind(ChatMessage msg) {
            if (msg.isUser()) {
                layoutUser.setVisibility(View.VISIBLE);
                layoutAi.setVisibility(View.GONE);
                tvUserMessage.setText(msg.getText());
            } else {
                layoutUser.setVisibility(View.GONE);
                layoutAi.setVisibility(View.VISIBLE);
                tvAiMessage.setText(msg.getText());

                // Inflate proposals
                containerProposals.removeAllViews();
                Context context = itemView.getContext();
                LayoutInflater inflater = LayoutInflater.from(context);

                for (AIProposal proposal : msg.getProposals()) {
                    View proposalCard = inflater.inflate(R.layout.item_proposal_card, containerProposals, false);
                    bindProposalCard(proposalCard, proposal, context);
                    containerProposals.addView(proposalCard);
                }
            }
        }

        private void bindProposalCard(View cardView, AIProposal proposal, Context context) {
            TextView tvOp = cardView.findViewById(R.id.tv_proposal_operation);
            TextView tvStatus = cardView.findViewById(R.id.tv_proposal_status);
            TextView tvPath = cardView.findViewById(R.id.tv_proposal_path);
            TextView tvContent = cardView.findViewById(R.id.tv_proposal_content);
            View layoutButtons = cardView.findViewById(R.id.layout_approval_buttons);
            MaterialButton btnApprove = cardView.findViewById(R.id.btn_approve_proposal);
            MaterialButton btnReject = cardView.findViewById(R.id.btn_reject_proposal);

            tvOp.setText(proposal.getType().getDisplayName());
            tvPath.setText(proposal.getTargetPath());

            if (proposal.getContent() != null && !proposal.getContent().isEmpty()) {
                tvContent.setText(proposal.getContent());
                tvContent.setVisibility(View.VISIBLE);
            } else {
                tvContent.setVisibility(View.GONE);
            }

            updateStatusUi(proposal, tvStatus, layoutButtons);

            btnApprove.setOnClickListener(v -> {
                try {
                    if (projectRoot != null) {
                        ApprovalManager.applyProposal(projectRoot, proposal);
                        updateStatusUi(proposal, tvStatus, layoutButtons);
                        Toast.makeText(context, "Approved & Applied: " + proposal.getTargetPath(), Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onProposalApproved(proposal);
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Approval Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            btnReject.setOnClickListener(v -> {
                ApprovalManager.rejectProposal(proposal);
                updateStatusUi(proposal, tvStatus, layoutButtons);
                Toast.makeText(context, "Proposal Rejected", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onProposalRejected(proposal);
            });
        }

        private void updateStatusUi(AIProposal proposal, TextView tvStatus, View layoutButtons) {
            switch (proposal.getStatus()) {
                case APPROVED:
                    tvStatus.setText("Approved");
                    tvStatus.setTextColor(Color.parseColor("#10B981"));
                    layoutButtons.setVisibility(View.GONE);
                    break;
                case REJECTED:
                    tvStatus.setText("Rejected");
                    tvStatus.setTextColor(Color.parseColor("#EF4444"));
                    layoutButtons.setVisibility(View.GONE);
                    break;
                case PENDING:
                default:
                    tvStatus.setText("Pending Approval");
                    tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                    layoutButtons.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }
}
