# Specialist Portal - Consultant Platform

Specialist-facing web application for managing consultations and availability.

## Tech Stack

- **Nuxt 3** - Vue.js framework
- **PrimeVue** - UI component library
- **TypeScript** - Type safety
- **Pinia** - State management
- **Tailwind CSS** - Styling

## Development

```bash
# Install dependencies
npm install

# Start development server (http://localhost:3002)
npm run dev

# Build for production
npm run build

# Run tests
npm run test

# Lint code
npm run lint
```

## Port Configuration

- Development: `3002`
- HMR: `24681`

## Environment Variables

```bash
NUXT_PUBLIC_API_BASE=http://localhost:8090/api
```

## Architecture

This app is specifically for specialists to:
- Manage their availability
- Handle consultation requests
- Update their profile and expertise
- Track consultation history
