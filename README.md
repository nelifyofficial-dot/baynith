# NeliPlay Android Application

**NeliPlay** is a premium native Android movie streaming and downloading application. It connects directly to the production Cloud Firestore database shared with NeliPlay Studio (`neliplay`).

---

## Architecture & Realtime Firestore Integration

* **Single Source of Truth**: Cloud Firestore (`neliplay`) is the sole production data source for movies, live TV channels, and categories.
* **Public Client / Zero Auth Requirement**: Users browse and stream public content immediately without account creation.
* **Security Rules Strict Compliance**:
  - `movies`: Queries strictly apply `.whereEqualTo("published", true)` to satisfy `allow read: if resource.data.published == true;`.
  - `tvChannels`: Queries strictly apply `.whereEqualTo("published", true)`.
  - `categories`: Public read access.
  - The Android app is strictly read-only and never writes, modifies, or deletes content.
* **Realtime Listener Synchronization**:
  - Uses Firestore snapshot listeners (`addSnapshotListener`).
  - When an administrator creates or edits a movie in NeliPlay Studio, the Android app updates in real-time without requiring an APK update.
  - When `published` changes from `false` to `true`, the movie appears in the app.
  - When `published` changes from `true` to `false`, the movie disappears from public movie lists.
* **Offline Firestore Cache**: Firestore offline persistence (`setPersistenceEnabled(true)`) caches metadata locally.

---

## Firestore Indexes (Firebase Console)

The application uses standard single-field queries and client-side ordering where possible to prevent missing index exceptions. For optimal database server-side sorting, the following composite indexes can be enabled in the Firebase Console:

1. **Collection `movies`**:
   - `published` (Ascending) + `createdAt` (Descending)
   - `published` (Ascending) + `year` (Descending)
   - `published` (Ascending) + `featured` (Ascending)
2. **Collection `tvChannels`**:
   - `published` (Ascending) + `order` (Ascending)
3. **Collection `categories`**:
   - `order` (Ascending)

---

## Media Playback & Download Specifications

* **Movies**: Progressive MP4 playback powered by AndroidX Media3 (ExoPlayer). Stream URL is obtained dynamically from `movies/{movieId}.streamUrl`.
* **Live TV**: HTTP Live Streaming (HLS / M3U8) via `tvChannels/{channelId}.streamUrl` with Media3 HLS module.
* **Downloads**:
  - Controlled by `movie.downloadEnabled == true`.
  - Background chunked download with HTTP Range header resume support.
  - Local playback using internal storage URIs with no active network required.
* **Continue Watching**: Playback progress saved in Room local database (`WatchProgressEntity`) and resumed automatically.

---

## Firebase Configuration

* **Project ID**: `neliplay`
* **Google Services File**: `app/google-services.json`
* **Package Name**: `com.neliplay.app`
* **Debug Verification**: On application launch, `FirebaseManager` logs connection status and queries 1 initial published document to verify Firestore reachability.

---

## Production Verification Flow

1. Open **NeliPlay Studio**.
2. Create a movie and set `published = true`.
3. Save the movie.
4. Launch **NeliPlay Android**.
5. The movie appears immediately via realtime Firestore listener.
6. Open movie details: `streamUrl` is retrieved from `movies/{movieId}`.
7. Tap "Watch": Media3 ExoPlayer plays the MP4.
8. If `downloadEnabled == true`, the download manager downloads the MP4 using `streamUrl`.
9. Set `published = false` in Studio: the movie instantly disappears from public movie lists.
