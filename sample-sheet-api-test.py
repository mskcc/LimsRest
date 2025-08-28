#!/usr/bin/env python3
"""
Sample Sheet API Test Script

This script demonstrates how to use the LimsRest Sample Sheet API
to generate sample sheets for Illumina sequencing runs.

Usage:
    python sample-sheet-api-test.py --run-id RUNID_123 --base-url http://localhost:8080
    
Requirements:
    pip install requests
"""

import argparse
import requests
import json
import sys
from typing import Dict, Any, Optional

class SampleSheetAPIClient:
    def __init__(self, base_url: str, token: Optional[str] = None):
        self.base_url = base_url.rstrip('/')
        self.token = token
        self.session = requests.Session()
        
        if token:
            self.session.headers.update({'Authorization': f'Bearer {token}'})
    
    def get_sample_sheet(self, 
                        run_id: str, 
                        flow_cell_id: Optional[str] = None,
                        experiment_id: Optional[str] = None,
                        format_type: str = 'table') -> Dict[str, Any]:
        """
        Get sample sheet data from the API
        
        Args:
            run_id: The sequencing run ID
            flow_cell_id: Optional flow cell ID
            experiment_id: Optional experiment ID
            format_type: 'table' for JSON data or 'csv' for CSV content
            
        Returns:
            Dictionary containing the API response
        """
        url = f"{self.base_url}/getSampleSheet"
        params = {
            'runId': run_id,
            'format': format_type
        }
        
        if flow_cell_id:
            params['flowCellId'] = flow_cell_id
        if experiment_id:
            params['experimentId'] = experiment_id
        
        try:
            response = self.session.get(url, params=params)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"API request failed: {e}")
            if hasattr(e, 'response') and e.response is not None:
                print(f"Response content: {e.response.text}")
            raise
    
    def save_csv_sample_sheet(self, run_id: str, output_file: str, **kwargs) -> None:
        """
        Generate and save a CSV sample sheet to file
        
        Args:
            run_id: The sequencing run ID
            output_file: Path to save the CSV file
            **kwargs: Additional parameters for get_sample_sheet
        """
        response = self.get_sample_sheet(run_id, format_type='csv', **kwargs)
        
        if not response.get('success', False):
            raise Exception(f"API call failed: {response.get('error', 'Unknown error')}")
        
        csv_content = response.get('csvContent', '')
        with open(output_file, 'w') as f:
            f.write(csv_content)
        
        print(f"Sample sheet saved to: {output_file}")
    
    def print_sample_summary(self, run_id: str, **kwargs) -> None:
        """
        Print a summary of the sample sheet data
        
        Args:
            run_id: The sequencing run ID
            **kwargs: Additional parameters for get_sample_sheet
        """
        response = self.get_sample_sheet(run_id, format_type='table', **kwargs)
        
        if not response.get('success', False):
            print(f"ERROR: {response.get('error', 'Unknown error')}")
            return
        
        data = response.get('sampleSheetData', {})
        samples = data.get('samples', [])
        warnings = data.get('warnings', [])
        
        print(f"\n=== Sample Sheet Summary ===")
        print(f"Run ID: {data.get('runId', 'N/A')}")
        print(f"Flow Cell ID: {data.get('flowCellId', 'N/A')}")
        print(f"Instrument: {data.get('instrument', 'N/A')}")
        print(f"Date: {data.get('date', 'N/A')}")
        print(f"Dual Barcoded: {data.get('isDualBarcoded', False)}")
        print(f"Read Lengths: {', '.join(data.get('reads', []))}")
        print(f"Barcode Mismatches: {data.get('barcodeMismatches1', 'N/A')}, {data.get('barcodeMismatches2', 'N/A')}")
        print(f"Total Samples: {len(samples)}")
        
        if warnings:
            print(f"\nWarnings ({len(warnings)}):")
            for warning in warnings:
                print(f"  - {warning}")
        
        if samples:
            print(f"\nSamples by Lane:")
            lanes = {}
            for sample in samples:
                lane = sample.get('lane', 'Unknown')
                if lane not in lanes:
                    lanes[lane] = []
                lanes[lane].append(sample)
            
            for lane, lane_samples in sorted(lanes.items()):
                print(f"  Lane {lane}: {len(lane_samples)} samples")
                for sample in lane_samples[:3]:  # Show first 3 samples
                    print(f"    - {sample.get('sampleId', 'N/A')} ({sample.get('recipe', 'N/A')})")
                if len(lane_samples) > 3:
                    print(f"    ... and {len(lane_samples) - 3} more")
        
        print()

def main():
    parser = argparse.ArgumentParser(description='Test the LimsRest Sample Sheet API')
    parser.add_argument('--run-id', required=True, help='Sequencing run ID')
    parser.add_argument('--base-url', default='http://localhost:8080', 
                       help='Base URL for the LimsRest API')
    parser.add_argument('--flow-cell-id', help='Flow cell ID (optional)')
    parser.add_argument('--experiment-id', help='Experiment ID (optional)')
    parser.add_argument('--token', help='Authentication token')
    parser.add_argument('--output-csv', help='Save CSV sample sheet to file')
    parser.add_argument('--output-json', help='Save JSON response to file')
    
    args = parser.parse_args()
    
    # Create client
    client = SampleSheetAPIClient(args.base_url, args.token)
    
    try:
        # Print summary
        client.print_sample_summary(
            args.run_id,
            flow_cell_id=args.flow_cell_id,
            experiment_id=args.experiment_id
        )
        
        # Save CSV if requested
        if args.output_csv:
            client.save_csv_sample_sheet(
                args.run_id,
                args.output_csv,
                flow_cell_id=args.flow_cell_id,
                experiment_id=args.experiment_id
            )
        
        # Save JSON if requested
        if args.output_json:
            response = client.get_sample_sheet(
                args.run_id,
                flow_cell_id=args.flow_cell_id,
                experiment_id=args.experiment_id,
                format_type='table'
            )
            
            with open(args.output_json, 'w') as f:
                json.dump(response, f, indent=2)
            print(f"JSON response saved to: {args.output_json}")
        
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main() 