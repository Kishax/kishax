"use client"

import { useEffect, useRef } from "react"

export default function DocsPage() {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // Dynamically import and initialize Scalar
    const initScalar = async () => {
      try {
        const { ApiReference } = await import("@scalar/api-reference")
        
        if (containerRef.current) {
          // Clear container
          containerRef.current.innerHTML = ""
          
          // Initialize Scalar API Reference
          const apiReference = new ApiReference(containerRef.current, {
            spec: {
              url: "/api/docs"
            },
            theme: "default",
            layout: "modern",
            showSidebar: true,
            customCss: `
              .scalar-app {
                height: 100vh;
              }
            `
          })
        }
      } catch (error) {
        console.error("Failed to load API documentation:", error)
        if (containerRef.current) {
          containerRef.current.innerHTML = `
            <div class="flex items-center justify-center h-screen">
              <div class="text-center">
                <h2 class="text-2xl font-bold text-red-600 mb-4">Failed to Load API Documentation</h2>
                <p class="text-gray-600">Please try refreshing the page.</p>
              </div>
            </div>
          `
        }
      }
    }

    initScalar()
  }, [])

  return (
    <div className="w-full h-screen">
      <div ref={containerRef} className="w-full h-full" />
    </div>
  )
}