# Stream `TIMELINE_INFORMAL` - Documentazione soluzione implementata

## Stato
Accepted

## Data
2026-07-30

## Contesto
Sulla piattaforma è stata introdotta una nuova tipologia di comunicazione (`informal`).
Era necessario notificare agli enti gli eventi di timeline mantenendo:

- minimo impatto evolutivo lato ente;
- effort di sviluppo contenuto;
- massimo riuso dell'architettura stream esistente.

Vincolo funzionale principale:
- una notifica può avere `communicationType = INFORMAL` oppure non averlo (flusso standard as-is).

## Obiettivo
Introdurre il supporto agli eventi `informal` senza alterare semanticamente i flussi esistenti `STATUS` e `TIMELINE`.

## Principi guida adottati
1. **Backward compatibility first**: nessuna regressione per enti già integrati.
2. **Opt-in esplicito**: adozione tramite stream dedicato.
3. **Separazione semantica**: informal isolato dal dominio timeline/status standard.
4. **Riuso architetturale**: modifiche minimali sui layer esistenti.
5. **Defense in depth**: filtro/routing sia infrastrutturale sia applicativo.

## Decisione architetturale
È stato adottato un **nuovo eventType dedicato**: `TIMELINE_INFORMAL`.

Motivazioni:
- evita side effect sugli stream standard già in uso;
- mantiene esplicita la semantica (tipo comunicazione != filtro categoria/stato);
- consente rollout graduale e rollback semplice.

## Alternative considerate

### A) Stream unico (standard + informal)
**Pro**
- nessun nuovo stream lato ente.

**Contro**
- rischio alto di regressione sui consumer esistenti;
- maggiore complessità contrattuale e di filtering;
- coupling tra domini con payload e regole diverse.

**Esito**: scartata.

### B) Stream dedicato `TIMELINE_INFORMAL` (scelta adottata)
**Pro**
- isolamento del rischio;
- adozione opt-in;
- elevato riuso dell'architettura.

**Contro**
- nuova configurazione stream lato ente.

**Esito**: accettata.

## Modifiche implementate per layer

### 1) API e contratti OpenAPI
- aggiunto `TIMELINE_INFORMAL` all'enum `eventType`;
- estesa la GET eventi per supportare payload informal dedicato;
- referenze schema informal allineate ai model sorgente in `pn-delivery`.

File:
- [pn-stream-api-external.yaml](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/docs/openapi/pn-stream-api-external.yaml)
- [pn-stream-api-internal.yaml](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/docs/openapi/pn-stream-api-internal.yaml)
- [remote-refs.yaml](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/docs/openapi/remote-refs.yaml)

### 2) Ingestion CDC e routing Lambda
- aggiornati filtri e routing su `communicationType`:
  - assente -> percorso standard
  - `INFORMAL` -> percorso informal
  - altri valori -> scarto esplicito
- EventSourceMapping aggiornati con pattern coerenti.

File:
- [kinesis.js](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/functions/streamEventManager/src/app/lib/kinesis.js)
- [kinesis.js](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/functions/notificationStreamEventManager/src/app/lib/kinesis.js)
- [microservice.yml](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/scripts/aws/cfn/microservice.yml)

### 3) Core matching stream-evento
- introdotta guardia di coerenza stream/evento:
  - evento informal solo su `TIMELINE_INFORMAL`
  - evento standard solo su `STATUS/TIMELINE`
- aggiunto branch dedicato `processInformalEvent`;
- gestione null-safe di `statusInfo` per eventi informal.

File:
- [StreamEventsServiceImpl.java](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/src/main/java/it/pagopa/pn/stream/service/impl/StreamEventsServiceImpl.java)
- [TimelineElementInternal.java](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/src/main/java/it/pagopa/pn/stream/dto/timeline/TimelineElementInternal.java)

### 4) Persistenza e path quarantena/sort
- propagato `communicationType` su entity e mapper DynamoDB webhook/quarantena;
- evitata perdita informazione durante replay dalla quarantena.

File:
- [WebhookTimelineElementEntity.java](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/src/main/java/it/pagopa/pn/stream/middleware/dao/timelinedao/dynamo/entity/webhook/WebhookTimelineElementEntity.java)
- [DtoToEntityWebhookTimelineMapper.java](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/src/main/java/it/pagopa/pn/stream/middleware/dao/mapper/DtoToEntityWebhookTimelineMapper.java)
- [EntityToDtoWebhookTimelineMapper.java](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/src/main/java/it/pagopa/pn/stream/middleware/dao/timelinedao/dynamo/mapper/webhook/EntityToDtoWebhookTimelineMapper.java)

## Architecture Decision Log (ADL)

### ADL-01 — Stream dedicato per informal
**Decisione**: introdurre `TIMELINE_INFORMAL`.
**Perché**: isolamento impatti e opt-in esplicito.

### ADL-02 — Nuovo eventType, non overload di filterValues
**Decisione**: separare dimensione "tipo comunicazione" da filtri categoria/stato.
**Perché**: semantica chiara e minore rischio regressioni.

### ADL-03 — Routing doppio (infra + app)
**Decisione**: applicare controlli in EventSourceMapping e in codice Lambda/service.
**Perché**: robustezza verso eventi non conformi.

### ADL-04 — Estensione GET eventi per schema informal
**Decisione**: supportare payload informal dedicato mantenendo compatibilità standard.
**Perché**: timeline/status informal hanno schema differente.

### ADL-05 — Propagazione `communicationType` anche in quarantena
**Decisione**: estendere entity/mapper con `communicationType`.
**Perché**: prevenire data-loss e scarti in replay.

## Retrocompatibilità e impatti
- nessuna modifica obbligatoria per gli enti già integrati su stream standard;
- adozione informal solo per chi crea stream `TIMELINE_INFORMAL`;
- comportamento standard preservato per eventi senza `communicationType`.

## Verifica tecnica eseguita
- test Lambda JS eseguiti e verdi:
  - `npm test --prefix functions/streamEventManager`
  - `npm test --prefix functions/notificationStreamEventManager`
- estensione test coverage Java su matching, mapper/quarantena, groups, sorting, consume event stream.
- report completo dei test disponibile in [test-report-timeline-informal.md](/Users/mario.gammaldi/Documents/GitHub/pn-stream.worktrees/stream-notification-approach-analysis/docs/test-report-timeline-informal.md).

Nota ambiente:
- build/test Maven completi non eseguibili localmente in questa sessione per dipendenza parent non risolta (`it.pagopa.pn:pn-parent:2.1.1`).

## Guida per sviluppatori (riferimento operativo)
1. Considerare informal come dominio separato dai flussi standard.
2. Mantenere invariata la guardia di matching tra stream type ed evento.
3. Ogni estensione di `communicationType` deve aggiornare insieme:
   - contratti OpenAPI;
   - routing CDC/Lambda;
   - matching core service;
   - mapper/entity di persistenza e quarantena;
   - test di isolamento/regressione.
4. Prima del merge, eseguire test in ambiente con parent Maven disponibile.

## Prossimo passo
Completare il task di rollout/fallback operativo con criteri go/no-go, KPI, alerting e procedura rollback.
