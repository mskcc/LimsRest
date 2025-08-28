# Sample Sheet API Implementation

This document describes the REST API implementation for generating Illumina sample sheets, adapted from the original `CreateSampleSheetSubmit` plugin.

## Overview

The Sample Sheet API provides a REST endpoint that generates Illumina sample sheets by querying the LIMS database and processing sample/barcode information. It supports both structured JSON output and ready-to-use CSV format.

## Components Created

### 1. REST Controller
**File**: `src/main/java/org/mskcc/limsrest/controller/GetSampleSheet.java`
- Handles HTTP requests to `/getSampleSheet`
- Validates parameters and manages authentication
- Returns structured responses with error handling

### 2. Service Layer
**File**: `src/main/java/org/mskcc/limsrest/service/GetSampleSheetTask.java`
- Contains the core business logic adapted from the original plugin
- Handles database queries and sample processing
- Manages machine-specific barcode transformations
- Implements intelligent barcode mismatch calculations

### 3. Model Classes
**Files**: 
- `src/main/java/org/mskcc/limsrest/model/SampleSheetData.java`
- `src/main/java/org/mskcc/limsrest/model/SampleSheetRow.java`

Data transfer objects for structured API responses.

### 4. Utility Classes
**File**: `src/main/java/org/mskcc/limsrest/util/SampleSheetUtils.java`
- Barcode processing utilities
- Hamming distance calculations for mismatch optimization
- Machine-specific padding and reverse complement operations
- IGO ID parsing and validation

### 5. Documentation & Testing
**Files**:
- `SAMPLE_SHEET_API.md` - Complete API documentation
- `sample-sheet-api-test.py` - Python test client

## Key Features Implemented

### Database Integration
- Connects to production LIMS database via VeloxConnection
- Queries multiple experiment types (HiSeq, NovaSeq, MiSeq, NextSeq)
- Handles flow cell lane assignments and sample hierarchies

### Barcode Processing
- **Dual Barcode Detection**: Automatically determines single vs dual barcoding
- **10x Genomics Support**: Special handling for Chromium and Visium samples
- **Machine-Specific Processing**: 
  - NovaSeq 6000: Reverse complement for i5 barcodes
  - NovaSeqX: No reverse complement needed
  - NextSeq/HiSeq: Machine-specific padding patterns
- **Intelligent Padding**: Uses barcode ID to determine correct padding sequences
- **Length Adjustment**: Automatic trimming/padding to match read lengths

### Sample Types Supported
- Standard Illumina library samples
- 10x Genomics single cell (Chromium, Visium)
- DLP single cell with chip position validation
- TCR sequencing (alpha/beta chains)
- SmartSeq single cell
- Fingerprinting samples
- Pooled samples with hierarchical resolution

### Data Validation & Quality Control
- **Barcode Collision Detection**: Warns about duplicate barcodes in lanes
- **Species Consistency**: Validates species within projects
- **Recipe Consistency**: Validates protocols within requests
- **Missing Data Detection**: Identifies samples with incomplete metadata
- **Hamming Distance Optimization**: Calculates optimal mismatch parameters

### Output Formats
1. **Structured JSON**: Full metadata for integration with other systems
2. **Illumina CSV**: Standard format ready for sequencer use

## API Usage

### Basic Usage
```bash
# Get structured data
curl -X GET "http://localhost:8080/getSampleSheet?runId=RUNID_123" \
  -H "Authorization: Bearer TOKEN"

# Get CSV format
curl -X GET "http://localhost:8080/getSampleSheet?runId=RUNID_123&format=csv" \
  -H "Authorization: Bearer TOKEN"
```

### Python Client
```python
from sample_sheet_api_test import SampleSheetAPIClient

client = SampleSheetAPIClient('http://localhost:8080', token='YOUR_TOKEN')
response = client.get_sample_sheet('RUNID_123')
client.save_csv_sample_sheet('RUNID_123', 'samplesheet.csv')
```

## Implementation Highlights

### Performance Optimizations
- Single database connection per request
- Efficient depth-first search for sample hierarchies
- Batch processing of barcode calculations
- Lazy loading of expensive operations

### Error Handling
- Comprehensive exception handling with meaningful error messages
- Graceful degradation when optional data is missing
- Detailed logging for troubleshooting
- Transaction safety with read-only database operations

### Security
- Requires ADMIN role authentication
- Input validation and sanitization
- No sensitive data in logs
- Read-only database operations

### Machine Type Support
The implementation handles sequencer-specific requirements:

| Machine Type | i5 Reverse Complement | Padding Strategy |
|--------------|----------------------|------------------|
| NovaSeq 6000 | Yes | Standard Illumina |
| NovaSeqX | No | Enhanced padding |
| NextSeq | Yes | Machine-specific |
| HiSeq 4000 | Yes | Legacy support |
| MiSeq | Yes | Standard |

### Advanced Features
- **Smart Barcode Mismatch Calculation**: Uses Hamming distance analysis
- **Pooled Sample Resolution**: Traverses complex sample hierarchies
- **Recipe-Specific Processing**: Custom logic for different assay types
- **Quality Metrics**: Comprehensive validation and warning system

## Database Schema Dependencies

The API queries these LIMS data types:
- `IlluminaSeqExperiment` / `IlluminaHiSeqExperiment` / `IlluminaMiSeqExperiment`
- `FlowCell` / `FlowCellLane`
- `Sample` / `IndexBarcode`
- `Request` / `Instrument`
- `NimbleGenHybProtocol` (for bait sets)
- `DLPLibraryPreparationProtocol1` (for DLP validation)

## Deployment Considerations

### Prerequisites
- Spring Boot application with Velox integration
- Database connection to LIMS production environment
- Authentication system (ADMIN role required)

### Configuration
The API uses existing LimsRest configuration:
- Database connection pooling via `ConnectionLIMS`
- Security configuration via `SecurityConfiguration`
- Logging configuration

### Monitoring
- All operations are logged with appropriate levels
- Performance metrics available via existing Spring Boot actuators
- Error rates trackable via exception logging

## Future Enhancements

### Potential Improvements
1. **RunInfo.xml Integration**: Parse actual run configuration files
2. **NovaSeqX DRAGEN Support**: Enhanced sample sheet generation
3. **Batch Processing**: Handle multiple runs in single request
4. **Caching Layer**: Cache experiment data for repeated requests
5. **WebSocket Support**: Real-time sample sheet updates
6. **Advanced Validation**: More sophisticated quality checks

### Integration Opportunities
- **Pipeline Automation**: Direct integration with demux pipelines
- **Dashboard Integration**: Real-time sample sheet monitoring
- **Quality Control Systems**: Automated validation workflows
- **Laboratory Information Systems**: Broader LIMS integration

## Maintenance

### Code Organization
- Clean separation of concerns (controller/service/model)
- Comprehensive unit testing potential
- Well-documented public APIs
- Extensible architecture for new features

### Documentation
- Complete API documentation with examples
- Inline code documentation
- Python client for testing and integration
- This implementation summary

This implementation provides a production-ready, scalable solution for generating Illumina sample sheets via REST API, maintaining compatibility with existing LIMS workflows while enabling modern integration patterns. 