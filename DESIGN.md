name: Harmonious Infrastructure
colors:
  surface: '#f8f9fa'
  surface-dim: '#d9dadb'
  surface-bright: '#f8f9fa'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f4f5'
  surface-container: '#edeeef'
  surface-container-high: '#e7e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#44474c'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#f0f1f2'
  outline: '#74777d'
  outline-variant: '#c4c6cd'
  surface-tint: '#4f6073'
  primary: '#041627'
  on-primary: '#ffffff'
  primary-container: '#1a2b3c'
  on-primary-container: '#8192a7'
  inverse-primary: '#b7c8de'
  secondary: '#944a00'
  on-secondary: '#ffffff'
  secondary-container: '#fc8f34'
  on-secondary-container: '#663100'
  tertiary: '#161616'
  on-tertiary: '#ffffff'
  tertiary-container: '#2a2a2a'
  on-tertiary-container: '#939190'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d2e4fb'
  primary-fixed-dim: '#b7c8de'
  on-primary-fixed: '#0b1d2d'
  on-primary-fixed-variant: '#38485a'
  secondary-fixed: '#ffdcc5'
  secondary-fixed-dim: '#ffb783'
  on-secondary-fixed: '#301400'
  on-secondary-fixed-variant: '#713700'
  tertiary-fixed: '#e5e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1c1b1b'
  on-tertiary-fixed-variant: '#474646'
  background: '#f8f9fa'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'

typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.25'
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.3'
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
  body-dense:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.3'
  label-bold:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  label-caps:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: 0.1em

rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px

spacing:
  unit: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 48px
  xxl: 80px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 48px
  max-width: 1280px

Brand & Style

The brand personality centers on "Harmonious Infrastructure"—a synthesis of
technical reliability and human-centric warmth. While the core functionality
addresses complex infrastructure, the interface prioritizes psychological
comfort, legibility, and long-session usability. The design style is Modern
Corporate with a Tactile twist. It avoids the sterile coldness of typical
DevOps tools by utilizing soft shadows, organic curves, and a grounded color
palette. The "Zebra" influence is manifested through rhythmic line work, subtle
patterns, and intentional high-contrast accents that guide the eye without
causing visual fatigue. The emotional response is one of calm confidence; the
user should feel supported by a system that is both sturdy and approachable.

Colors

This design system utilizes a grounded, high-stability palette.

* Primary (Midnight Blue): Used for structural elements, primary navigation, and headers to establish a sense of "Infrastructure."
* Secondary (Terracotta): Used sparingly for call-to-actions, status indicators, and highlights to inject human warmth and focus.
* Background (Soft Off-White): Applied to all large surfaces to minimize blue light strain and provide a "paper-like" reading experience.
* Zebra Accent (Rich Black): Reserved for high-precision typography and thin decorative strokes to maintain clarity and brand rhythm.

Typography

Plus Jakarta Sans is selected for its approachable, geometric clarity and
friendly terminals. Hierarchy is established through weight and generous
vertical rhythm. A 1.6x line height is mandated for general body text to ensure maximum
readability. For data-dense environments like logs, monitoring consoles, and complex tables, the `body-dense` variant (1.3x line height) is used to maximize vertical screen space and information density. Large headlines use tighter tracking and
heavier weights to provide the "Zebra" contrast against the softer body content.
Captions and labels use slightly increased letter spacing and semi-bold weights
to ensure legibility at small scales.

Layout & Spacing

The layout follows a Fluid-Fixed hybrid model. On desktop, content is contained
within a 1280px max-width container to prevent line lengths from becoming
excessive. Spacing follows an 8px base grid to ensure mathematical harmony.

* Desktop: 12-column grid with 24px gutters. Use large xl (48px) padding for section separation to provide visual "breathing room."
* Tablet: 8-column grid with 24px gutters and 32px side margins.
* Mobile: 4-column grid with 16px gutters and 16px margins. Headlines should scale down to mobile variants defined in the typography section.

Elevation & Depth

Elevation is achieved through Ambient Shadows rather than borders. This creates
a softer, more organic transition between the background and interactive
elements.

* Level 0 (Base): Soft Off-White (#F8F9FA). Flat.
* Level 1 (Cards/Floating UI): White (#FFFFFF) surface with a soft, diffused shadow: 0px 4px 12px rgba(26, 43, 60, 0.05).
* Level 2 (Overlays/Modals): White (#FFFFFF) surface with a deeper shadow: 0px 12px 32px rgba(26, 43, 60, 0.12). 

Shapes

The shape language is consistently "Rounded." This softens the technical nature
of the infrastructure data.

* Standard Elements (Buttons, Inputs): 0.5rem (8px) radius.
* Large Elements (Cards, Containers): 1rem (16px) radius.
* Feature Elements (Promos, Hero Cards): 1.5rem (24px) radius. Interactive elements should never be sharp-edged. The only sharp elements allowed are the internal 1px "Zebra" rhythm lines used for data separation.

Components

Buttons

* Primary: Midnight Blue fill, White text, 8px radius. On hover, apply a soft, diffused Terracotta outer glow (e.g., 0px 0px 8px rgba(252, 143, 52, 0.4)) or a slight color brightening to signal focus without breaking the rounded shape language.
* Secondary: Transparent fill, Midnight Blue 1px border, 8px radius.
* Accent: Terracotta fill for critical "human" actions (e.g., Support, Help, New Project).

Inputs & Forms

* Fields: White background with a subtle 1px border in a muted version of Midnight Blue.
* Focus State: 2px Terracotta outline with a soft outer glow.
* Labels: Always positioned above the field using label-bold.

Cards

* White background, 16px radius, Level 1 shadow.
* Internal padding should be generous (md or lg).
* Zebra patterns should be used extremely sparingly. Restricted to specialized infra-tracking cards to prevent visual clutter and Moiré effects on lower-resolution screens.

Lists & Data

* Rows are separated by thin 1px lines in 10% Midnight Blue.
* Alternating row backgrounds should use 2% Midnight Blue to mimic a subtle zebra stripe effect for easier data scanning without overwhelming the user.

Chips & Tags

* Used for status indicators. Use light tints of the success/error colors with bold text for high contrast. 100px (pill) radius.