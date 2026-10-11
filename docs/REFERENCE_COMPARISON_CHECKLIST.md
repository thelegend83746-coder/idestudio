# IDE STUDIO — Reference UI Reproduction & Comparison Checklist

## 1. Design Principles & Recording-Overlay Exclusion
As required by the PRD:
- **Zero Recording Overlays**: No artificial touch circles, pointer icons, or software recording indicators are rendered.
- **Genuine Feedback**: Standard Android ripple animations, tactile button feedback, and native scroll behaviors are preserved throughout.
- **Color Scheme**: Deep dark charcoal backgrounds (`#18181A`, `#1E1F22`, `#2B2D30`) with vibrant primary blue (`#3884FF`), secondary green (`#00C853`), and AI purple (`#8A2BE2`).

---

## 2. Screen-by-Screen Comparison Inventory

| Screen | Observable Reference Feature | IDE STUDIO Implementation | Status |
| :--- | :--- | :--- | :--- |
| **Home Screen** | App title branding & logo | `TopAppBar` with code symbol badge and "IDE STUDIO" title | PASS |
| | Settings icon in top right | `IconButton` navigating to `SettingsScreen` | PASS |
| | Prominent Create Project action | Floating action button with `+` icon and text | PASS |
| | Project items list | `ProjectCard` with name, package name, last modified date | PASS |
| | Project context menu | Options menu with "Open", "Rename", and "Delete" | PASS |
| | Empty state | Distinct folder illustration, helpful guidance, and New Project button | PASS |
| **Create Project: Step 1** | Template selection cards | Cards for Simple App, Full Screen App, Navigation Drawer App, Basic Activity, Empty Activity (all routing to the single unified robust template generator) | PASS |
| | Selection highlight | Rounded card with border accent and checkmark badge | PASS |
| | Navigation controls | Back button and Next step button | PASS |
| **Create Project: Step 2** | App Name input | Text field with instant validation (letters, numbers, underscores) | PASS |
| | Package Name input | Text field with syntax validation (`com.example.app`) | PASS |
| | Save Location | Displays `/storage/emulated/0/test-folder/projects` | PASS |
| | Min SDK selector | Dropdown selector with APIs (21 to 34) and Android version labels | PASS |
| | Language indicator | Labelled "Java (Android Native)" | PASS |
| | Create action | Validates inputs, creates genuine project files, navigates to editor | PASS |
| **Code Editor** | Active filename & project | Displayed in top navigation bar | PASS |
| | File tabs | Horizontal scrolling tabs with dirty-dot indicators and close buttons | PASS |
| | Line numbers column | Left gutter synced with vertical scrolling | PASS |
| | Syntax highlighting | Token-based syntax highlighting for Java and XML | PASS |
| | Quick code toolbar | Mobile symbols row (`{`, `}`, `(`, `)`, `;`, `"`, `=`, `<`, `>`, `.`, Tab) | PASS |
| | Autocomplete suggestions | Context-sensitive chip suggestions for Java & XML keywords | PASS |
| | Toolbar actions | Undo, Redo, Save, Build AI, Build APK, Settings, Explorer toggle | PASS |
| **Project Explorer** | Hierarchical file tree | Expandable/collapsible directories with tree indentation | PASS |
| | Filetype icons | Specialized icons for Java, XML, Manifest, Images, and Folders | PASS |
| | File management | New File, New Folder, Rename, Delete with safety checks | PASS |
| | Safe paths | Validates against directory traversal (`..`) | PASS |
| **Build Log Output** | Expandable bottom drawer | Header bar with stage status, collapse/expand toggle, cancel button | PASS |
| | Real-time stage updates | 10 distinct build pipeline stages displayed dynamically | PASS |
| | Real compiler output | Uncut log stream with timestamps and log levels | PASS |
| | Diagnostic error cards | Clickable error cards that navigate to source file and error line | PASS |
| | Output artifact banner | Displays APK file path, size in KB, "Install APK", and "Share APK" | PASS |
| **Build AI Settings** | Ollama Cloud API Key | Masked input field with password toggle and secure storage | PASS |
| | Test Connection | Genuine HTTP request to provider endpoint (`/api/tags`), latency check | PASS |
| | Model selection | Dropdown selector with discovered models and popular defaults | PASS |
| | System instructions | Multiline editable text field preconfigured for Android Java/XML | PASS |
| | Save settings | Persists configuration reliably across application restarts | PASS |
| **Settings** | Categories menu | Application, Editor, Build & Run, Build AI, About Us | PASS |
| | Editor preferences | Font Size (slider), Tab Size (2/4/8), Line Numbers, Wrap, Dark Theme | PASS |
| | Build & Run settings | Platform API availability table with honest READY/MISSING statuses | PASS |
| | About Us | Version 1.0.0, architecture summary, and technologies | PASS |
