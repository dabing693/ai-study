import { join } from "node:path"
import { defineConfig } from "vite"
import dotenv from "dotenv"
import nitro from "./nitro.config"
import { projectDir } from "./shared/dir"

dotenv.config({
  path: join(projectDir, ".env.server"),
})

export default defineConfig({
  resolve: {
    alias: {
      "@shared": join(projectDir, "shared"),
    },
  },
  plugins: [
    nitro(),
  ],
})
