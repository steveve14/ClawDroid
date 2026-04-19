```markdown
# Design System Strategy: The Ethereal Intelligence

## 1. Overview & Creative North Star
This design system is built to transform a standard AI interface into a premium, editorial experience. The Creative North Star is **"The Ethereal Intelligence"**—a philosophy that balances the soft, human-centric tones of deep violets and lavenders with the sharp, technical precision of an advanced assistant. 

We move beyond the "template" look by rejecting rigid structural lines in favor of **Tonal Architecture**. By using intentional asymmetry, overlapping surfaces, and a sophisticated hierarchy of depth, we create a workspace that feels less like a chat app and more like a high-end digital atelier. This system is designed to breathe, utilizing expansive white space and high-contrast typography to ensure that the AI's intelligence feels organized, authoritative, and calm.

---

## 2. Colors: Tonal Architecture
The palette centers on a sophisticated spectrum of purples. Instead of using lines to separate thoughts, we use the shifting weight of color.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section off content. Physical boundaries must be defined solely through background color shifts or subtle tonal transitions. For example, a chat history section should be defined by a `surface_container_low` background sitting against a `surface` main background.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked, physical layers. 
- **Base:** Use `surface` (#FEF7FF) for the main background.
- **Floating Context:** Use `surface_container_lowest` (#FFFFFF) for elements that need to feel "closer" to the user, like an active card or an input field.
- **Deep Content:** Use `surface_container_high` (#EDE6EE) for secondary sidebars or background groupings.
- **The Glass Rule:** For floating elements like the `NavigationBar`, use the `surface` color at 80% opacity with a heavy `backdrop-blur` (16px–24px). This creates a "frosted glass" effect that allows the conversation colors to bleed through softly.

### Signature Textures
Avoid flat UI. Use a subtle linear gradient (Top-Left to Bottom-Right) for Primary CTAs:
- **Gradient Start:** `primary` (#4F378A)
- **Gradient End:** `primary_container` (#6750A4)
This creates a "pulse" of energy that flat colors cannot replicate.

---

## 3. Typography: Editorial Precision
This design system utilizes a high-contrast pairing of **Manrope** for structural authority and **Inter** for conversational clarity, punctuated by **JetBrains Mono** for technical data.

- **The Headline Scale:** Use `display-md` (2.75rem) for welcome states and `headline-lg` (2rem) for major section headers. The goal is to feel like a high-end magazine.
- **The Chat Flow:**
    - **Headers:** `title-lg` (1.375rem / 22sp) in Manrope.
    - **Messages:** `body-lg` (1rem / 16sp) in Inter for maximum legibility.
    - **Code Blocks:** `JetBrains Mono`. This is non-negotiable for AI responses to provide a distinct visual "mode" for technical information.
- **Hierarchy:** Use `on_surface_variant` (#494551) for labels and timestamps to keep the focus on the primary conversation.

---

## 4. Elevation & Depth: Tonal Layering
We do not use shadows to create "pop"; we use color weight.

- **The Layering Principle:** Depth is achieved by "stacking" surface tiers. Place a `surface_container_lowest` card on a `surface_container_low` background to create a soft, natural lift.
- **Ambient Shadows:** If a "floating" effect is required (e.g., a Modal), use an extra-diffused shadow. 
    - **Blur:** 32px
    - **Opacity:** 6%
    - **Color:** Tinted with `primary` (#4F378A) rather than pure black. This mimics natural light passing through the violet-tinted glass.
- **The "Ghost Border":** If a boundary is required for accessibility, use the `outline_variant` token at **20% opacity**. Never use a 100% opaque border.

---

## 5. Components: Intentional Styling

### Chat Bubbles (The Core Component)
Bubbles should feel like soft-weighted pillows.
- **User Bubbles:** Use `secondary_container` (#E8DEF8). 
    - **Corner Radius:** Top-Left: XL, Top-Right: XL, Bottom-Left: XL, Bottom-Right: SM (Creates a "directional" tail).
- **AI Bubbles:** Use `surface_container_highest` (#E7E0E8).
    - **Corner Radius:** Top-Left: XL, Top-Right: XL, Bottom-Left: SM, Bottom-Right: XL.
- **Spacing:** No dividers. Use 16dp of vertical breathing room between different speakers.

### NavigationBar
- **Background:** `surface` at 90% opacity with backdrop-blur.
- **Indicator:** Use a pill-shaped `primary_container` for the active state, but ensure the icon is `on_primary_container`.
- **Layout:** Use asymmetrical spacing—offset the "New Chat" button slightly higher to draw the eye as the primary action.

### Buttons & AssistChips
- **Primary Button:** Large (XL) rounded corners (1.5rem). Use the Signature Gradient.
- **AssistChips:** Use the `surface_container_highest` for the background. Do not use an outline. These should feel like "pills" floating in the space.
- **Input Field:** A `surface_container_lowest` container with a "Ghost Border" (10% `outline`).

### ElevatedCard
- **Style:** No shadow. Use a 1-tier shift in surface color (e.g., `surface_container_low` card on a `surface` background) to define the edge. Use an XL corner radius.

---

## 6. Do’s and Don’ts

### Do:
- **Do** use `JetBrains Mono` for all AI-generated technical content.
- **Do** allow content to bleed to the edges of the screen if it’s an image or code block; it breaks the "boxed-in" feel.
- **Do** use `primary_fixed` for subtle highlights in text that need emphasis without the weight of a bold font.

### Don’t:
- **Don't** use 100% black (#000000) for text. Always use `on_surface` to maintain the violet-tinted softness.
- **Don't** use dividers or horizontal rules. Separate content blocks with 32dp or 48dp of vertical white space.
- **Don't** use standard Material 3 "Medium" rounding (8dp). This system requires **XL (24dp)** or **Full (999px)** rounding to achieve the "Ethereal" look.
- **Don't** stack more than three levels of surface containers (e.g., Surface -> Low -> High). Any more creates visual "noise."

---

## 7. Interaction Note
When the AI is "thinking," do not use a standard spinner. Use a subtle, pulsing gradient transition between `primary` and `primary_container` across the entire background of the AI bubble to indicate life and activity.```