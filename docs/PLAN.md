# Cabin Architecture & Plan

## Architecture
- **Single Activity App**: Using Jetpack Compose Navigation.
- **MVVM Pattern**: ViewModel exposing StateFlows. Repository pattern to abstract data sources.
- **Database**: Room for local storage, FTS4 for full-text search.
- **Networking/Scraping**: OkHttp for fetching HTML, Readability4J for extracting content, Jsoup for sanitizing.
- **Background Work**: WorkManager for reliable offline-first background saving.
- **DI**: Koin for lightweight dependency injection.

## Data Model (Room)
`ArticleEntity`
- id (UUID string) - Primary Key
- url
- canonicalUrl
- title
- byline
- siteName
- excerpt
- lang
- contentHtml (stored with local image links)
- wordCount
- readingMinutes
- leadImagePath
- savedAt (timestamp)
- readAt (timestamp, null if unread)
- archived (boolean)
- favorite (boolean)
- scrollPosition (integer)
- status (enum: QUEUED, SAVING, READY, FAILED, LINK_ONLY)
- error (string, null)

`ArticleFtsEntity` (FTS4 table)
- rowid
- textContent (extracted text for search)

## Milestones
1. Scaffold Android project structure (Gradle, dependencies, manifest).
2. Setup Room Database and DAOs.
3. Setup Network (OkHttp), Readability4J, and HTML sanitization/image extraction (Jsoup).
4. Implement WorkManager for saving pipeline (fetch, parse, download images, update DB).
5. Implement ViewModels and Compose UI (Library screen, Reader screen, Flight-readiness screen).
6. Implement Intent sharing (ACTION_SEND).
7. Write Unit and Instrumented Tests.
8. Configure CI/CD (GitHub Actions).
9. Update README.md with instructions.
