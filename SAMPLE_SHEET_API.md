# Sample Sheet API Documentation

## Overview

The Sample Sheet API provides a REST endpoint for generating Illumina sample sheets directly from the LIMS database. This API is adapted from the `CreateSampleSheetSubmit` plugin and provides both structured JSON data and CSV format outputs.

**🔒 READ-ONLY OPERATION**: This API performs only read operations on the database. No data is modified, created, or deleted. All operations are queries and data retrieval only.

## Endpoint

```
GET /getSampleSheet
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `runId` | String | Yes | The sequencing run ID |
| `flowCellId` | String | No | The flow cell ID (will be derived from run if not provided) |
| `experimentId` | String | No | The experiment ID (will be derived from runId if not provided) |
| `format` | String | No | Response format: "table" (default) or "csv" |

## Authentication

Requires `ADMIN` role access.

## Response Formats

### Table Format (Default)

Returns structured JSON data with sample sheet information:

```json
{
  "runId": "RUNID_123",
  "flowCellId": "FLOWCELL_456",
  "success": true,
  "format": "table",
  "sampleSheetData": {
    "runId": "RUNID_123",
    "flowCellId": "FLOWCELL_456", 
    "instrument": "DIANA",
    "date": "12/15/2023",
    "application": "DIANA",
    "reads": ["151", "151"],
    "barcodeMismatches1": 1,
    "barcodeMismatches2": 1,
    "isDualBarcoded": true,
    "samples": [
      {
        "lane": "1",
        "sampleId": "Sample1_IGO_12345_1",
        "samplePlate": "Human", 
        "sampleWell": "WES_Human",
        "indexId": "IDT_i7_001",
        "indexTag": "TAAGGCGA",
        "indexTag2": "CGTACTAG",
        "sampleProject": "Project_12345",
        "baitSet": "MSK-ACCESS-v1_0-probes-Agilent",
        "description": "investigator@mskcc.org",
        "igoId": "12345_1",
        "otherId": "Sample1",
        "recipe": "WES_Human",
        "species": "Human",
        "requestId": "12345",
        "pi": "investigator@mskcc.org"
      }
    ],
    "warnings": []
  }
}
```

### CSV Format

Returns CSV content ready for use with Illumina sequencers:

```json
{
  "runId": "RUNID_123",
  "flowCellId": "FLOWCELL_456",
  "success": true,
  "format": "csv",
  "csvContent": "[Header]\nDate,12/15/2023\nApplication,DIANA\nReads1,151\nReads2,151\nBarcodeMismatchesIndex1,1\nBarcodeMismatchesIndex2,1\n\n[Data]\nLane,Sample_ID,Sample_Plate,Sample_Well,Index_ID,Index_Tag,Index_Tag2,Sample_Project,Bait_Set,Description\n1,Sample1_IGO_12345_1,Human,WES_Human,IDT_i7_001,TAAGGCGA,CGTACTAG,Project_12345,MSK-ACCESS-v1_0-probes-Agilent,investigator@mskcc.org\n"
}
```

## Example Usage

### Get structured sample sheet data
```bash
curl -X GET "http://localhost:8080/getSampleSheet?runId=RUNID_123" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Get CSV format
```bash
curl -X GET "http://localhost:8080/getSampleSheet?runId=RUNID_123&format=csv" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Specify flow cell ID
```bash
curl -X GET "http://localhost:8080/getSampleSheet?runId=RUNID_123&flowCellId=FLOWCELL_456" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Features

### Barcode Processing
- Automatic detection of single vs dual barcoded samples
- Support for 10x Genomics barcode formats
- Machine-specific barcode transformations (reverse complement, padding)
- Intelligent barcode mismatch calculation based on Hamming distances

### Sample Types Supported
- Standard Illumina sequencing samples
- 10x Genomics single cell samples (Chromium, Visium)
- DLP single cell samples with chip position validation
- TCR sequencing samples
- SmartSeq samples
- Fingerprinting samples

### Machine Type Support
- NovaSeq 6000/X Plus
- NextSeq 500/550/1000/2000
- HiSeq 2500/4000
- MiSeq

### Data Validation
- Duplicate barcode detection
- Species consistency checking within projects
- Recipe consistency checking within projects
- Missing metadata warnings

## Error Handling

### Error Response Format
```json
{
  "runId": "RUNID_123",
  "flowCellId": null,
  "success": false,
  "error": "No experiment found for runId: RUNID_123"
}
```

### Common Error Scenarios
- **No experiment found**: Run ID doesn't exist in LIMS
- **No flow cells found**: Experiment exists but has no associated flow cells
- **No lanes found**: Flow cell exists but has no lane assignments
- **No samples found**: Lanes exist but have no sample assignments
- **Database connection issues**: LIMS database is unavailable

## Implementation Notes

### Database Queries
The API queries multiple LIMS data types to find experiments:
- `IlluminaSeqExperiment`
- `IlluminaHiSeqExperiment`
- `IlluminaMiSeqExperiment`
- `IlluminaNextSeqExperiment`

### Sample Processing
Uses depth-first search to traverse sample hierarchies and resolve pooled samples to individual barcoded samples.

### Performance Considerations
- Response time depends on sample count and complexity
- Typically 1-5 seconds for standard runs
- May be slower for highly pooled runs or complex sample hierarchies

## Integration

This API can be integrated with:
- Sequencing pipeline automation systems
- LIMS workflow management tools
- Laboratory information dashboards
- Quality control systems

## Security

- **🔒 READ-ONLY DATABASE ACCESS**: All operations are strictly read-only
  - No data modifications, creations, or deletions
  - Only queries and data retrieval operations
  - Safe for production use without risk of data corruption
- **Authentication**: Requires ADMIN role access
- **Data Protection**: No sensitive data is logged in plain text
- **Audit Trail**: All operations are logged for security monitoring
- **Transaction Safety**: Uses read-only database transactions
- **Follows standard MSKCC security protocols**

### Database Operations Performed
- ✅ `queryDataRecords()` - Read data from database tables
- ✅ `getStringVal()`, `getLongVal()`, `getBooleanVal()` - Retrieve field values
- ✅ `getChildrenOfType()`, `getParentsOfType()` - Navigate relationships
- ❌ **NO** `setValue()`, `setDataField()` - No data modifications
- ❌ **NO** `createDataRecord()`, `addDataRecord()` - No record creation
- ❌ **NO** `deleteDataRecord()` - No record deletion
- ❌ **NO** `commit()`, `save()` - No transaction commits 