# Design System & Style Guide

## Overview
This document records the style decisions and design system used in the CampusWall frontend refactor. The design aims for a modern, clean, and "glassmorphism" inspired look, using a soft color palette and rounded geometry.

## Color Palette

### Primary Colors
- **Primary Gradient**: `from-purple-600 to-blue-500` (Used in Logo, Buttons)
- **Secondary Gradient**: `from-pink-500 to-orange-400` (Used in Text Highlights)

### Semantic Colors
- **Confession (表白)**: Pink (`text-pink-500`, `bg-pink-50`)
- **TreeHole (树洞)**: Green (`text-green-500`, `bg-green-50`)
- **Help (求助)**: Blue (`text-blue-500`, `bg-blue-50`)
- **Market (市集)**: Orange (`text-orange-500`, `bg-orange-50`)
- **Lost & Found (失物)**: Purple (`text-purple-500`, `bg-purple-50`)

### Backgrounds
- **Base**: `bg-base-200` (Light Gray/Off-white for main background)
- **Cards**: `bg-base-100` (White) with `shadow-sm` or `shadow-lg`
- **Hero**: `bg-gradient-to-br from-purple-50 via-white to-blue-50`

## Typography
- **Font Family**: Inter, system-ui, sans-serif
- **Headings**: Bold/ExtraBold, often with gradients.
- **Body**: Slate-500/600 for softer reading text.

## UI Components

### Navbar
- **Style**: Sticky, Glassmorphism (`backdrop-blur-md`, `bg-base-100/80`).
- **Elements**: Logo on left, Centered Links with Icons, Right-aligned Actions.

### Buttons
- **Primary**: Rounded full (`rounded-full`), Gradient background, Shadow.
- **Ghost/Text**: Used for secondary actions and navigation links.

### Cards
- **Style**: Rounded-xl or 2xl (`rounded-2xl`), subtle borders (`border-base-200`), hover effects (`hover:-translate-y-1`, `hover:shadow-md`).

### Animations
- **Fade In Up**: Used for Hero text entrance.
- **Float**: Used for background shapes and hero visual elements.
- **Pulse Soft**: Used for background blurs.

## Environment Variables
- `VITE_APP_TITLE`: Application Name (Default: CampusWall)
- `VITE_API_BASE_URL`: API Endpoint Base

## Refactor Notes
- Moved specific page content (Sidebar items) from Layout to Views using `Teleport`.
- Implemented a responsive grid system.
- Standardized icon usage (SVG).
