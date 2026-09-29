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
