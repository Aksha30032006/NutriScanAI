# NutriScan AI (Spring Boot + MySQL)

Same frontend and same API as the Node version, with a Java Spring Boot 3 backend and MySQL database.

## Needed
- JDK 17 or newer (`java -version`)
- Maven 3.9+ (`mvn -v`)
- MySQL 8 running on localhost:3306

## Run
1. Start MySQL. The database `nutriscan` is created automatically.
2. Open `src/main/resources/application.properties` and set your MySQL password
   (or set the env var `DB_PASSWORD`; username defaults to `root`).
3. In the project folder run:
   ```
   mvn spring-boot:run
   ```
4. Open http://localhost:8080

In VS Code: install "Extension Pack for Java" and "Spring Boot Extension Pack", open this folder, then run `NutriScanApplication`.

## Optional: real photo detection
Set `ANTHROPIC_API_KEY` before starting. Without it, the food name you type (or the file name) is matched against the built-in list.

## Layout
- `src/main/java/com/nutriscan`: entities, repositories, JWT auth, ApiController, FoodService
- `src/main/resources/static`: the 15 HTML pages, CSS and JS
- `uploads/`: scanned food images
- Put your Figma food collage at `src/main/resources/static/img/bg.jpg` for the background.

## Deploy (so the layout and API work online)
All page links, CSS and JS use relative paths, so the pages work from any folder or sub-path.

**Option A: whole app on one host (recommended)**
Host Spring Boot (Railway, Render, a VPS). It serves both pages and API, so leave `static/js/config.js` as `window.API_BASE = ''`.
Set these environment variables on the host:
- `DB_URL` = `jdbc:mysql://HOST:3306/DBNAME?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET` = any long random text
- `ANTHROPIC_API_KEY` (optional, for AI)
- `PORT` is set by most hosts automatically.
Build command: `mvn clean package -DskipTests`. Start command: `java -jar target/nutriscan-ai-1.0.0.jar`

**Option B: pages on Netlify / GitHub Pages, backend elsewhere**
1. Upload only the `static` folder contents to the static host.
2. Open `js/config.js` and set `window.API_BASE = 'https://your-backend-url';`
3. The backend already allows cross-origin requests (CORS).

## Opening pages locally
Do not double-click the HTML files for the full app. Start the backend and open http://localhost:8080.
(With VS Code Live Server the pages automatically call http://localhost:8080.)
