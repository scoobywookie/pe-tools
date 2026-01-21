# PE Tools: Civil Engineering Automation Dashboard

### IB Computer Science Internal Assessment (2026)

**Client:** Place Engineering, Raleigh, NC
**Developer:** Daniel Puckett

![Java](https://img.shields.io/badge/Java-17%2B-orange) ![Maven](https://img.shields.io/badge/Build-Maven-blue) ![Platform](https://img.shields.io/badge/Platform-Windows-lightgrey)

## Project Overview

**PE Tools** is a custom desktop application designed to automate the workflow of civil engineers. It solves the specific problem of manually locating, downloading, and formatting GIS (Geographic Information System) data for AutoCAD Civil 3D.

By integrating a **JavaFX** front-end with a compiled **Python** back-end, the application reduces a 20-minute manual task to a 45-second automated process, while providing a centralized hub for project management and site visualization.

---

## Key Features

### 1. AutoCAD Automation Engine

- **Input:** Accepts a standard street address.
- **Process:** Triggers a bundled background executable (`address_to_scr.exe`) to query the Raleigh Open Data API.
- **Output:** Automatically generates a formatted `.scr` (AutoCAD Script) file that draws property boundaries, building footprints, and infrastructure lines directly into Civil 3D.
- **Real-Time Logging:** Displays a live, non-blocking console log in the GUI using background threading.

### 2. Project Management

- **Smart Note-Taking:** A rich-text editor (HTML-based) that automatically organizes notes by week.
- **Data Persistence:** Notes are automatically saved to the local file system (`~/.petools/notes`) and restored upon launch.
- **Shortcuts:** Custom keybindings for engineering formatting (Checkboxes, Time-stamps).

### 3. Site Locator

- Embedded **WebView** (Google Maps integration) allowing engineers to visually verify site locations before running automation scripts.

### 4. "Self-Healing" Architecture

- The application includes a robust resource management system. Upon launch, it checks for critical external dependencies (like the automation executable). If missing, it extracts fresh copies from the internal JAR resources to the user's local directory, ensuring the tool is "unbreakable."

---

## Technical Stack

- **Language:** Java 17 (OpenJDK)
- **GUI Framework:** JavaFX (Modular implementation)
- **Build Tool:** Apache Maven
- **Automation Logic:** Python (Pandas, GeoPandas, Shapely) compiled to `.exe` via PyInstaller.
- **Data Format:** JSON (API), HTML (Notes), SCR (AutoCAD).

---

## Project Structure

```text
src/main/java/com/petools
├── App.java                 # Entry point
├── layout/                  # Main UI structure
│   ├── MainLayout.java
│   └── Sidebar.java         # Persistent navigation menu
├── features/                # Functional modules
│   ├── autocad/             # Automation logic & ProcessBuilder
│   ├── projects/            # Note-taking system
│   ├── sitelocator/         # Map WebView
│   └── todo/                # Task management
└── resources/
    ├── images/              # Assets (Logos, Icons)
    └── scripts/             # Compiled Python executables
```
