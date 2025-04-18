# Utility di sblocco per gli eventi da pn-WebhookEventsQuarantine

Questo script permette di sbloccare tutti gli elementi associati agli stream con sorting abilitato dalla tabella DynamoDB ppn-WebhookEventsQuarantine.

## Prerequisiti

- Node.js >= 18.0.0 installato
- Configurazione AWS CLI con le credenziali appropriate

## Utilizzo

```bash
node index.js --envName|-e <ambiente>
```

### Parametri

- `--envName`, `-e`: Ambiente di destinazione (dev|uat|test|prod|hotfix)
- `--help`, `-h`: Visualizza il messaggio di aiuto

### Esempi

```bash
# Aggiornamento degli stream in ambiente dev
node index.js -e dev
```

## Output

Lo script produce un riepilogo dell'esecuzione con:
- Numero totale di stream con sorting enabled recuperati
- Numero di messaggi mandati
- Numero di aggiornamenti falliti

## Note

Al fine di verificare che lo script sia stato eseguito correttamente controllare che nella quarantena non ci sia più nessun elemento