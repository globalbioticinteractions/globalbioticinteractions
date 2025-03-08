import json
import pandas as pd
import os

# List all JSON files in the current directory
json_files = [file for file in os.listdir() if file.endswith('.json')]

# List to hold all rows from all JSON files
all_table_data = []

# Process each JSON file
for json_file in json_files:
    print(f"Processing : {json_file}")
    with open(json_file, "r") as file:
        json_data = json.load(file)
    
    # Check if 'fields' key exists
    if 'fields' in json_data:
        for field in json_data['fields']:
            row = {
                "title": json_data.get("title", "")
            }
            # Add all key-value pairs from each field dictionary to the row
            row.update(field)
            all_table_data.append(row)
    else:
        print(f"'fields' key not found in {json_file}")

# Creating a DataFrame with all the collected data
df = pd.DataFrame(all_table_data)

# Define the output CSV file path
csv_filename = "combined_fields_table.csv"
df.to_csv(csv_filename, index=False)

print(f"CSV file saved as: {csv_filename}")
