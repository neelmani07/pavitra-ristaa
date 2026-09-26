import { defineConfig } from "@neon/config/v1";

export default defineConfig({
  // Declare your Neon services here
  auth: false,
  // Private: every read still goes through the app's own presigned URLs (see MediaUrlResolver) -
  // a public bucket would let anyone fetch a photo pending/rejected moderator approval directly,
  // bypassing that check entirely.
  buckets: {
    "pavitra-media-staging": { access: "private" },
  },
  // Branch policy: per-branch tuning
  branch: (branch) => {
    if (branch.isDefault) {
      // Default branch: no overrides, uses project defaults
      return {};
    }
    if (!branch.exists) {
      // New non-default branches: auto-expire
      // Run `neon checkout <name>` to create a new branch with these settings
      return { ttl: "7d" };
    }
    // Existing branch: no changes
    return {};
  },
});
