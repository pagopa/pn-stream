# Utility di migrazione degli stream efil

Questo script permette di aggiornare il flag di sorting e la data di attivazione degli stream passati in input.

## Prerequisiti

- Node.js >= 18.0.0 installato
- Configurazione AWS CLI con le credenziali appropriate

## Utilizzo

```bash
node index.js --envName|-e <ambiente> --filename|-f filename.json --inputDate|-d <data> --tableName|-t <streamTable> | -m | --help|-h
```

### Parametri

- `--envName`, `-e`: Ambiente di destinazione (dev|uat|test|prod|hotfix)
- `--filename`, `-f`: Il file con i record da aggiornare
- `--inputDate`, `-d`: Data di attivazione degli stream, obbligatoriamente nel formato UTC
-  `--testMode`, `-m`: non aggiorna il campo
- `--tableName`, `-t`: Nome della tabella degli stream
- `--help`, `-h`: Visualizza il messaggio di aiuto

      
### Esempi

```bash
# Aggiornamento degli stream in ambiente dev
node index.js -e dev -f streams.json -d 2025-03-20T13:53:22
```

## Output

Lo script produce un riepilogo dell'esecuzione con:
- Numero totale di streamIds
- Numero totale di streamId aggiornati
- Numero di aggiornamenti falliti

## Note