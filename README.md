# ebmcrawler
This program goes through the EBM catalog and extracts information on medical procedures.

Information extracted is:
- id of the procedure
- title of the procedure
- regulations for billing of the procedure
- billing exceptions for the procedure (which other procedures cannot be billed together with this one in which circumstances)

The resulting information is output as a CSV-file.

## Usage
```
java -jar ebmcrawler-0.1.jar <EBM_HTML_DIR> <OUTPUT_FILE>
```

The EBM html directory can be accessed via [this](https://www.kbv.de/media/EBMBrowserHTML.zip) zipped download. In the extracted zip, the html folder is simply called `html`.