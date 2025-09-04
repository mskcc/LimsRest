# Bulk Banked Samples Controller

## Overview

The `SetBankedSamples` controller provides a bulk operation endpoint for creating or updating multiple banked samples in a single API call. Unlike the individual `/setBankedSample` endpoint, this controller processes all samples in a single batch database operation, which is much more efficient for bulk operations.

## Endpoint

- **URL**: `/setBankedSamples`
- **Method**: `POST`
- **Content-Type**: `application/json`

## Request Format

The request body should contain a JSON object with an array of banked sample objects:

```json
{
  "samples": [
    {
      "userId": "SAMPLE_001",
      "user": "testuser",
      "igoUser": "igo_user",
      "serviceId": "SERVICE_001",
      "rowIndex": "1",
      "transactionId": "12345",
      // ... other fields
    },
    {
      "userId": "SAMPLE_002",
      "user": "testuser",
      "igoUser": "igo_user",
      "serviceId": "SERVICE_002",
      "rowIndex": "2",
      "transactionId": "12346",
      // ... other fields
    }
  ]
}
```

## Required Fields

Each sample object must contain these required fields:
- `userId`: The sample identifier
- `user`: The user performing the operation
- `igoUser`: The IGO user
- `serviceId`: The service identifier
- `rowIndex`: The row index
- `transactionId`: The transaction identifier

## Optional Fields

All other fields are optional and will use default values if not provided:
- `index` (default: "NULL")
- `barcodePosition` (default: "NULL")
- `vol` (default: "-1.0")
- `concentration` (default: "-1.0")
- `concentrationUnits` (default: "NULL")
- `sequencingReadLength` (default: "NULL")
- `numTubes` (default: "NULL")
- `assay` (default: ["NULL"])
- `clinicalInfo` (default: "NULL")
- `collectionYear` (default: "NULL")
- `gender` (default: "NULL")
- `knownGeneticAlteration` (default: "NULL")
- `organism` (default: "NULL")
- `species` (default: organism value)
- `preservation` (default: "NULL")
- `specimenType` (default: "NULL")
- `sampleType` (default: "NULL")
- `sampleOrigin` (default: "NULL")
- `micronicTubeBarcode` (default: "NULL")
- `sampleClass` (default: "NULL")
- `spikeInGenes` (default: "NULL")
- `tissueType` (default: "NULL")
- `cancerType` (default: "")
- `recipe` (default: "NULL")
- `capturePanel` (default: "NULL")
- `runType` (default: "NULL")
- `investigator` (default: "NULL")
- `cellCount` (default: "NULL")
- `naToExtract` (default: "NULL")
- `coverage` (default: "NULL")
- `seqRequest` (default: "NULL")
- `rowPos` (default: "NULL")
- `colPos` (default: "NULL")
- `plateId` (default: "NULL")
- `tubeId` (default: "NULL")
- `patientId` (default: "NULL")
- `normalizedPatientId` (default: "NULL")
- `cmoPatientId` (default: "NULL")
- `numberOfAmplicons` (default: "0")

## Response Format

The response contains information about the bulk operation results:

```json
{
  "results": [
    "Record Id:12345",
    "Record Id:12346"
  ],
  "errors": [
    "Sample SAMPLE_003 missing required field: userId"
  ],
  "totalProcessed": 3,
  "totalSuccess": 2,
  "totalErrors": 1
}
```

### Response Fields

- `results`: Array of successful operation results (Record IDs)
- `errors`: Array of error messages for failed operations
- `totalProcessed`: Total number of samples processed
- `totalSuccess`: Number of successfully processed samples
- `totalErrors`: Number of samples that failed to process

## Validation

The controller performs the following validations:

1. **Required Fields**: Checks that all required fields are present
2. **Format Validation**: Uses existing whitelist validation for `userId` and `serviceId`
3. **Data Type Validation**: Validates numeric fields can be parsed correctly

## Error Handling

- Individual sample failures don't stop the processing of other samples
- Detailed error messages are provided for each failed sample
- The response includes summary statistics of the operation

## Example Usage

### cURL Example

```bash
curl -X POST \
  http://localhost:8080/setBankedSamples \
  -H 'Content-Type: application/json' \
  -d @example-bulk-banked-samples.json
```

### JavaScript Example

```javascript
const response = await fetch('/setBankedSamples', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    samples: [
      {
        userId: "SAMPLE_001",
        user: "testuser",
        igoUser: "igo_user",
        serviceId: "SERVICE_001",
        rowIndex: "1",
        transactionId: "12345",
        // ... other fields
      }
    ]
  })
});

const result = await response.json();
console.log(`Processed ${result.totalProcessed} samples`);
console.log(`Success: ${result.totalSuccess}, Errors: ${result.totalErrors}`);
```

## Performance Considerations

- The controller processes all samples in a single batch database operation
- This is much more efficient than processing samples individually
- All samples are committed to the database in a single transaction
- Large batches are processed efficiently with minimal database round trips
- Consider breaking very large batches into smaller chunks if needed for memory management

## Security

- The controller uses the same security model as the individual `SetBankedSample` endpoint
- All validation rules and whitelist checks are applied to each sample
- The `@PreAuthorize("hasRole('ADMIN')")` annotation is inherited from the `SetOrCreateBanked` service

## Migration from Individual Endpoint

To migrate from individual `/setBankedSample` calls to the bulk endpoint:

1. Collect all sample data into an array
2. Format the request according to the JSON structure above
3. Make a single POST request to `/setBankedSamples`
4. Handle the response to check for any individual failures

This approach reduces network overhead and improves performance for bulk operations.
