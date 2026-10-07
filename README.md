# Manga Library API — relazione tecnica

API REST per gestire un catalogo di manga, una libreria personale e l'avanzamento della lettura, con importazione dei metadati da MyAnimeList.

## Obiettivi

Il sistema centralizza i metadati dei manga e separa il catalogo condiviso dalla libreria di ciascun utente. Consente di registrarsi, consultare e importare manga, gestire autori e generi e aggiornare il proprio avanzamento. Questa relazione descrive la versione `0c1dac0`, includendo il confronto con le versioni precedenti.

## Tecnologie e architettura

Java 21, Spring Boot 3.4.1, Maven Wrapper 3.9.16, Spring Data JPA, Spring Security e Springdoc 2.7.0. Il database applicativo è MySQL 8.0. Il progetto genera un WAR eseguibile.

Le richieste attraversano i controller REST, i servizi di dominio e i repository JPA. I package `author`, `genre`, `manga` e `user` organizzano le funzionalità; `integration/mal` contiene il client MyAnimeList, `config` le configurazioni e `exceptions` la gestione degli errori.

La separazione tra controller, servizi e repository distingue il protocollo HTTP dalle regole di dominio e dalla persistenza, permettendo test unitari dei servizi e test di integrazione delle richieste. Le operazioni di registrazione, cambio ruolo, importazione e aggiornamento dell'avanzamento sono transazionali. La documentazione delle API è generata con OpenAPI; l'autenticazione è gestita da Spring Security.

## Modello dati

| Entità | Funzione e relazioni |
| --- | --- |
| `User` | Account con username ed email univoci, password codificata e ruolo. |
| `UserProfile` | Biografia e avatar; relazione uno a uno con l'utente. |
| `Manga` | Titolo, descrizione, anno, capitoli e volumi; un autore e più generi. |
| `Author` | Nome, cognome e biografia; può essere associato a più manga. |
| `Genre` | Categoria in relazione molti a molti con i manga. |
| `UserMangaProgress` | Associa utente e manga, registrando stato, capitolo e volume correnti. |

Gli stati di lettura sono `PLAN_TO_READ`, `READING`, `COMPLETED`, `ON_HOLD` e `DROPPED`. Rimuovere un manga dalla libreria elimina solo l'avanzamento personale. Un autore collegato a manga non può essere eliminato.

L'importazione aggiorna i manga cercandoli per titolo e riutilizza autori e generi già presenti. Se un titolo richiesto nella libreria non è disponibile localmente, il servizio tenta di importarlo da MyAnimeList.

Il client esterno invia il client ID nell'header `X-MAL-CLIENT-ID`, limita la ricerca a un massimo di 100 risultati e mappa descrizione, capitoli, volumi, generi e primo autore. I conteggi pari a zero vengono salvati come valori assenti. Le associazioni JPA rappresentano le relazioni tra entità; lo schema iniziale MySQL è disponibile in `sql/db.sql`.

## API e sicurezza

| Percorso | Operazioni | Accesso |
| --- | --- | --- |
| `/api/registration` | POST: registrazione | Pubblico |
| `/login` | POST: login tramite form | Pubblico |
| `/api/manga`, `/api/manga/{id}` | GET: catalogo e dettaglio | Autenticato |
| `/api/manga/import` | POST: importazione con parametri `query` e `limit` | Autenticato |
| `/api/manga/{id}` | DELETE: eliminazione dal catalogo | ADMIN |
| `/api/author`, `/api/genre` | GET e POST: elenco e creazione | Autenticato |
| `/api/author/{id}`, `/api/genre/{id}` | PUT e DELETE: modifica ed eliminazione | Autenticato |
| `/api/users/{userId}/manga` | GET e POST: libreria e avanzamento | Proprietario |
| `/api/users/{userId}/manga/{mangaId}` | DELETE: rimozione dalla libreria | Proprietario |
| `/api/admin/dashboard`, `/api/admin/users` | GET: dashboard ed elenco utenti | ADMIN |
| `/api/admin/users/{id}` | DELETE: eliminazione utente | ADMIN |
| `/api/admin/users/{id}/role` | PUT: modifica ruolo tramite parametro `role` | ADMIN |
| `/v3/api-docs`, `/swagger-ui/index.html` | Documentazione OpenAPI | Autenticato |

La registrazione richiede `username`, `email` e `password` non vuoti, con email valida. Il servizio codifica la password con BCrypt e assegna sempre **`ROLE_USER`**, ignorando un eventuale ruolo inviato dal client. Solo un amministratore può modificare il ruolo; i valori ammessi sono `ROLE_USER` e `ROLE_ADMIN`.

L'autenticazione usa HTTP Basic o login tramite form, che reindirizza alla libreria personale. Le password non vengono restituite nel JSON. Il controllo di proprietà impedisce anche agli amministratori di accedere alla libreria altrui.

### Esempi di richieste

Registrazione pubblica con `Content-Type: application/json`:

```http
POST /api/registration
```

```json
{"username":"lettore","email":"lettore@example.com","password":"scegli-una-password"}
```

La risposta riuscita è HTTP 200 con il messaggio `Successfully registered! ID: <id>`. Un eventuale campo `role` nel JSON non permette di ottenere privilegi amministrativi.

Aggiornamento dell'avanzamento, autenticandosi come proprietario della libreria:

```http
POST /api/users/{userId}/manga
```

```json
{"mangaTitle":"Naruto","status":"READING","currentChapter":5,"currentVolume":1}
```

La risposta HTTP 200 contiene l'avanzamento salvato. I dati di registrazione non validi producono HTTP 400; l'accesso alla libreria altrui produce 403; un manga locale inesistente richiesto per ID produce 404. Le eccezioni generiche vengono gestite centralmente con HTTP 500. I dettagli delle richieste e dei modelli sono consultabili tramite OpenAPI e la raccolta Postman.

## Installazione e avvio

Servono Git, un JDK completo 21 e Docker con Compose. Clonare il repository, entrare nella cartella e compilare per eseguire i test e generare il WAR:

```bash
git clone https://github.com/yuukni/manga-library-api.git
cd manga-library-api
./mvnw clean verify
```

### MySQL e Docker Compose

Creare un file `.env`, ignorato da Git, con `MYSQL_DATABASE`, `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD` e `MAL_CLIENT_ID`, usando valori propri. Le prime quattro variabili configurano il database e l'account applicativo; `MAL_CLIENT_ID` identifica l'applicazione registrata su [MyAnimeList](https://myanimelist.net/apiconfig) e serve per l'importazione esterna. Non salvare credenziali nel repository.

```bash
./mvnw clean verify
docker compose up --build
```

La build deve precedere Compose: il Dockerfile copia il WAR da `target/`. I servizi usano le porte 3306 (MySQL), 8080 (API) e 8081 (phpMyAdmin). `sql/db.sql` inizializza un volume MySQL nuovo; l'hash segnaposto dell'amministratore va sostituito con un hash BCrypt valido. Il percorso Docker/Tomcat non è stato verificato in questa revisione.

## Test e coverage

JUnit, Mockito e MockMvc verificano servizi, controller, registrazione, autorizzazioni e libreria personale. I test di integrazione usano il profilo `test` e rollback transazionale.

### Confronto tra i commit

Le misure sono state ottenute rieseguendo le suite originali di ciascun commit con Java 21 e JaCoCo 0.8.13, in copie temporanee dei sorgenti, senza modificarne codice, test o dipendenze e senza aggiungere esclusioni. Tutte le esecuzioni sono terminate con `BUILD SUCCESS` e senza fallimenti o errori.

| Commit | Evoluzione | Test superati | Test saltati | Coverage righe | Coverage rami |
| --- | --- | --- | --- | --- | --- |
| `7e55dd3` | Prima dell'ampliamento dei test | 7 | 1 | **11,19%** (32/286) | **11,54%** (6/52) |
| `488ff4f` | Test unitari ampliati e correzione del ruolo | 93 | 0 | **83,11%** (251/302) | **78,79%** (52/66) |
| `0c1dac0` | Test di sicurezza, proprietà della libreria e validazione | 118 | 0 | **85,71%** (270/315) | **78,38%** (58/74) |

Il codice applicativo di `7e55dd3` è identico a quello di `b12453e`: tra i due commit cambia solo il README. Nel primo confronto il runner conta 8 casi, di cui uno disabilitato e 7 superati.

Rispetto alla versione precedente all'ampliamento, la coverage delle righe aumenta di **74,52 punti percentuali**, quella dei rami di **66,84 punti**. Le percentuali si riferiscono al codice di ogni versione: nel frattempo aumentano anche le righe e i rami misurabili. Tra `488ff4f` e `0c1dac0` i rami coperti passano da 52 a 58, mentre il totale passa da 66 a 74; questo spiega la lieve diminuzione percentuale della coverage dei rami.

### Verifica del ruolo in registrazione

In `7e55dd3`, `registerUser` usava `form.getRole()`. Il commit `488ff4f` rimuove il ruolo dal form pubblico, impone `ROLE_USER` nel servizio e introduce l'aggiornamento amministrativo con whitelist. Il commit `0c1dac0` verifica mediante richieste HTTP che inviare `ROLE_ADMIN` durante la registrazione crei comunque un utente standard, con password codificata e senza accesso alle funzioni amministrative. Verifica anche che solo un amministratore possa modificare i ruoli e che la libreria sia accessibile esclusivamente al proprietario.

### Riproduzione delle misure

La configurazione Maven di questi tre commit non include JaCoCo né impone una soglia minima: `./mvnw clean verify` esegue i test, ma non genera automaticamente un report di coverage. Per misurare la versione scelta, dalla sua cartella eseguire:

```bash
./mvnw clean \
  org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent \
  test \
  org.jacoco:jacoco-maven-plugin:0.8.13:report
```

Il report viene generato in `target/site/jacoco/index.html`, con dati XML nella stessa cartella. `clean` impedisce di riutilizzare dati di coverage precedenti. JaCoCo applica i propri filtri standard al codice generato.

I risultati dei test sono disponibili in `target/surefire-reports/`. La raccolta `Manga-Backend-API.postman_collection.json` permette verifiche manuali delle API.
