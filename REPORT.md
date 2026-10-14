# Lab 2 Web Server -- Project Report

## What I specified

What each task had to do and how it is checked:

- **Error page.** An unknown path requested with 'Accept: text/html' returns a 404 with my own page instead of the Spring whitelabel page, showing the status and the requested path. A test with a real server ('TestRestTemplate') checks the status and the content.
- **'/time'.** A GET returns 200 and a JSON body with a 'time' field. With '?zone=' it returns the current time in that zone, and an unknown zone gets a 400. Three 'MockMvc' tests cover these cases.
- **HTTP/2 over TLS.** The app serves HTTPS on port 8443 and agrees on HTTP/2 with the client: 'curl -v' should show 'ALPN: server accepted h2' and an 'HTTP/2' status line.
- **Build.** './gradlew check' passes, ktlint included.

## What I changed

One commit per task, so the history is easy to follow:

- **3eef0ec – error page.** 'templates/error.html' shows the status code in large type, the error name, a small table (status, error, path, time), links back to '/' and to '/time', and a footer. 'ErrorPageTest.kt' checks the 404, the footer text, the status and the path. 'src/test/resources/application.yml' turns SSL off for the tests.
- **25bcff7 – /time.** 'TimeComponent.kt' contains 'TimeDTO' (with 'time' and 'zone'), 'TimeProvider' and 'TimeService', the 'toDTO()' extension and 'TimeController' with an optional 'zone' parameter, all documented with KDoc. 'TimeControllerTest.kt' checks the JSON, a request for 'Asia/Tokyo' and an invalid zone.
- **772d2fc – HTTP/2 and TLS.** The keystore 'localhost.p12' and 'application.yml' move the server to port 8443 with SSL, set the key alias and enable HTTP/2.
- **915e4ef – ktlint.** Reformats 'build.gradle.kts' so that ktlint passes on the starter.

## Technical decisions

**A general error message.** Spring uses 'error.html' for every error, not only 404, so a sentence like "this page does not exist" would be wrong for a 500. The page shows the values Spring already provides ('status', 'error', 'path', 'timestamp'), which also covers step further 1. 'message' is left out because it is empty by default.

**The zone in the JSON.** For '/time' I went for the zone option of step further 2. A 'LocalDateTime' carries no zone, so the response includes the zone id too; without a parameter the server zone is used. An unknown zone makes the conversion to 'ZoneId' fail and Spring answers 400 by itself, so no extra error handling was needed. The fixed-clock test, the other option in the guide, was not done.

**Testing with Tokyo.** The server runs in 'Europe/Madrid', so a test with Madrid would pass even if the parameter were ignored. The test asks for 'Asia/Tokyo' and accepts up to one minute of difference with the current Tokyo time.

**The certificate.** I also did step further 3. The certificate lists 'IP:127.0.0.1' next to 'DNS:localhost', because the checks connect to 127.0.0.1, and 'server.ssl.key-alias' is 'localhost', the name given to 'openssl pkcs12 -name'. It was created with '-days 365' instead of the default 30 days, so it does not expire before grading.

**Tests and git.** Tests run over plain HTTP because the test 'application.yml' disables SSL. 'localhost.crt', 'localhost.key' and 'openssl-localhost.cnf' stay out of git, while 'localhost.p12' is committed because the app needs it to start.

## How I verified

'.\gradlew ktlintCheck' and '.\gradlew check' end with BUILD SUCCESSFUL, and the 5 tests pass.

Then, with '.\gradlew bootRun' running in one terminal, I ran these checks from a second one (Windows PowerShell):

```
curl.exe -v --http2 -k -H "Accept: text/html" -i https://127.0.0.1:8443/
curl.exe -v --http2 -k -i https://127.0.0.1:8443/time
curl.exe -k "https://127.0.0.1:8443/time?zone=Asia/Tokyo"
```

- The first one shows a TLSv1.3 handshake, 'ALPN: server accepted h2', 'HTTP/2 404', 'text/html' and my page (title "404 Not Found · Lab 2", path '/' in the table).
- The second one shows 'ALPN: server accepted h2', 'HTTP/2 200', 'application/json' and {"time":"2026-10-14T12:50:24.06309","zone":"Europe/Madrid"}.
- The third one returns {"time":"2026-10-14T19:50:24.1775384","zone":"Asia/Tokyo"}, seven hours ahead of Madrid.

curl also prints "SSL certificate verification failed, continuing anyway!". That is expected: the certificate is self-signed, which is why the guide uses '-k'.

What failed first:

- ktlint already failed on the starter's 'build.gradle.kts' (line 28), and the '/time' code from the guide did not pass it either. '.\gradlew ktlintFormat' fixed both.
- 'ErrorPageTest' failed with 'error.html should contain "Lab 2 error page"' until the template existed.
- The Windows 'curl.exe' refused '--http2' ("the installed libcurl version does not support this"), so I switched to the curl build from curl.se (winget package 'cURL.cURL'), which includes nghttp2.
- My first curl attempt gave "Connection refused" because the server was not running yet.

## AI disclosure

- **Tools / skills:** Claude Code in VS Code.
- **Purpose:** resolving the initial `ktlint` configuration failure in the starter template (commit `915e4ef`); troubleshooting how to run `curl` with HTTP/2 support in Windows PowerShell; consulting Thymeleaf syntax details to format the technical details table in `error.html`; formatting KDoc documentation comments; and proofreading/translating the draft notes for `REPORT.md` from Spanish into English.
- **Representative prompts** (translated from Spanish): *"Fix this ktlint failure in the starter with the minimal change"*; *"How can I run curl with HTTP/2 support in Windows PowerShell?"*; *"How to reference status and path attributes in a Thymeleaf table layout?"*; *"Proofread these technical notes and translate them into English for REPORT.md"*.
- **Affected files/sections:** `build.gradle.kts` (initial ktlint fix), `error.html` (HTML/Thymeleaf layout structure), `TimeComponent.kt` (KDoc comments), and `REPORT.md` (language review and formatting).
- **Validation steps:** I ran `./gradlew ktlintCheck` and `./gradlew check` locally after every change (verifying that all 5 tests pass), started the application to test TLS and HTTP/2 negotiation, and manually executed the three `curl` verification checks (see *How I verified*).
- **Citations:** Base architecture and reference code adapted from `docs/GUIDE.md`, including the `ErrorPageTest` skeleton, `TimeDTO`, `TimeProvider`, `TimeService`, `toDTO()`, `TimeController`, `TimeControllerTest`, the OpenSSL configuration and commands, and `application.yml`.
- **Human-reviewed:** All core implementation and design decisions were performed and verified by the author, including selecting and building the custom error page layout, implementing the `zone` parameter logic and unit tests, generating the self-signed certificate and `.p12` keystore, configuring `application.yml` (commits `3eef0ec`, `25bcff7`, `772d2fc`), and verifying the final outputs against the guide with `curl`.