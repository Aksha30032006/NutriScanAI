// Where the backend lives.
// - Same server serves pages + API (npm start / mvn spring-boot:run): leave as ''.
// - Pages hosted separately (Netlify, GitHub Pages, Live Server): put your backend URL, e.g. 'https://your-app.onrender.com'
window.API_BASE = '';
if (!window.API_BASE && (location.protocol === 'file:' || location.port === '5500' || location.port === '5501')) {
  window.API_BASE = 'http://localhost:8080'; // Spring Boot default. Use 3000 for the Node version.
}
