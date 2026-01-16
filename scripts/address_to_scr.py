# Daniel Puckett & Joe Puckett | Place Engineering, PLLC
# v1.5 - Updated for Java Automation
# This script is "Frozen" ready for PyInstaller (.exe conversion)

import requests
from geopy.geocoders import Nominatim
import geopandas as gpd
import pandas as pd
import sys
import os
import io

sys.stdout.reconfigure(encoding='utf-8')

# --- 1. DYNAMIC PATH SETUP ---
# Detect User Desktop automatically
USER_DESKTOP = os.path.join(os.path.expanduser("~"), "Desktop")
OUTPUT_FOLDER = os.path.join(USER_DESKTOP, "CAD-IMPORTS")

# Ensure folder exists
if not os.path.exists(OUTPUT_FOLDER):
    try:
        os.makedirs(OUTPUT_FOLDER)
    except OSError:
        pass # Handle potential race conditions

def suppress_warnings():
    import warnings
    warnings.filterwarnings("ignore", message=".*attribute name.*")
    warnings.filterwarnings("ignore", message=".*truncated when saved to ESRI Shapefile.*")
    warnings.filterwarnings("ignore", message=".*created as date field.*")
    warnings.filterwarnings("ignore", message=".*Normalized/laundered field name.*")
    warnings.filterwarnings("ignore", message=".*Field UPDATE_DAT.*")
    warnings.filterwarnings("ignore", message=".*update_dat.*")
    try:
        warnings.filterwarnings("ignore", message="Field .* create as date field, though DateTime requested.", category=RuntimeWarning, module="pyogrio\\.raw")
        warnings.filterwarnings("ignore", message=".*not successfully written.*", category=UserWarning, module="pyogrio.raw")
    except:
        pass

def get_coords_nconemap(address):
    try:
        geolocator = Nominatim(user_agent="city_county_lookup_combined")
        geo_result = geolocator.geocode(address, addressdetails=True)
        
        city = county = None
        if geo_result and 'address' in geo_result.raw:
            addr = geo_result.raw['address']
            city = addr.get('city') or addr.get('town') or addr.get('village') or addr.get('hamlet')
            county = addr.get('county')
        
        # Next, get x and y from NC OneMap (ArcGIS)
        url = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates"
        params = {
            "SingleLine": address,
            "f": "json",
            "outSR": "2264",  # NC State Plane (ftUS)
            "maxLocations": 1
        }
        response = requests.get(url, params=params)
        data = response.json()

        if data.get('candidates'):
            location = data['candidates'][0]['location']
            return location['x'], location['y'], city, county
    except Exception as e:
        print(f"❌ Geocoding Error: {e}")
        return None, None, None, None

    return None, None, city, county

def generate_script(x, y, shapefile_paths):
    radius = 5000
    # Save script to the dynamic Output Folder
    script_path = os.path.join(OUTPUT_FOLDER, "circle_layers.scr")
    
    # Path to the import profile (Assumes it lives in the same folder, or you can hardcode a shared network path)
    # If the .ipf file doesn't exist, Civil 3D might prompt the user, so be aware.
    ipf_path = os.path.join(OUTPUT_FOLDER, "gis data.ipf")

    try:
        if os.path.exists(script_path):
            os.remove(script_path)
            
        with open(script_path, "w") as f:
            f.write("CIRCLE\n")
            f.write(f"{x},{y}\n")
            f.write(f"{radius}\n")
            f.write(f"ZOOM\nC\n{x},{y}\n{2 * radius}\n")
            
            # Note: The original logic had a specific block for the first item. 
            # I kept the logic but updated the paths.
            if len(shapefile_paths) > 0:
                f.write(f"-MAPIMPORT\n")
                f.write(f"shp\n")
                f.write(f"{shapefile_paths[0]}\n") # This path is now dynamic
                f.write("yes\n")
                # Using dynamic IPF path (ensure this file exists on Dad's computer or remove this line)
                f.write(f"{ipf_path}\n") 
                f.write("proceed\n")
                f.write('(load "apply_topo_elevation.lsp") AssignTopoElevation\n')
            
            for shp in shapefile_paths:
                # Avoid re-importing the first one if logic overlaps, but keeping your original loop structure
                if "topo" not in shp: 
                    f.write(f"-MAPIMPORT\n")
                    f.write(f"shp\n")
                    f.write(f"{shp}\n")
                    f.write("yes\n")
                    f.write(f"{ipf_path}\n")
                    f.write("proceed\n")
            
            f.write('(load "enable_linetype_generation.lsp") EnableLinetypeGeneration\n')
            
        print(f"📁 Script generated: {script_path}")
    except Exception as e:
        print(f"❌ Failed to write script: {e}")

def get_layer(x, y, layer_name, url):
    # print(" " * 80, end='\r') # Removed clear line for Java console compatibility
    print(f"📦 Fetching layer: {layer_name.split('s')[0]}")
    
    total_collected = 0
    # Save Shapefile to dynamic Output Folder
    out_path = os.path.join(OUTPUT_FOLDER, f"{layer_name}.shp")
    
    offset = 0
    limit = 2000
    all_gdfs = []
    
    while True:
        params = {
            "geometry": f"{x-5000},{y-5000},{x+5000},{y+5000}",
            "geometryType": "esriGeometryEnvelope",
            "inSR": 2264,
            "spatialRel": "esriSpatialRelIntersects",
            "outFields": "*",
            "returnGeometry": True,
            "resultOffset": offset,
            "f": "json"
        }
        try:
            response = requests.get(url, params=params)
            response.raise_for_status()
        except requests.exceptions.RequestException as e:
            print(f"❌ Failed to retrieve {layer_name} data.")
            return None
    
        try:
            # Check if response is valid JSON before parsing geometry
            if "error" in response.text:
                 print(f"❌ API Error for {layer_name}")
                 return None
                 
            gdf = gpd.read_file(io.StringIO(response.text))
        except Exception as e:
            # Often happens if no features are found
            # print(f"ℹ️ No features or read error for {layer_name}")
            return None

        if gdf.empty:
            return None
        
        all_gdfs.append(gdf)
        count = len(gdf)
        total_collected += count
        
        # Simple progress for Java Console
        if total_collected % 500 == 0:
            print(f"   ... {total_collected} items found")
            
        if count < limit:
            break
        offset += limit
    
    if not all_gdfs:
        print(f"⚠️ No {layer_name} features found.")
        return None
    
    try:
        final_gdf = pd.concat(all_gdfs, ignore_index=True)
        if not final_gdf.is_valid.all():
            final_gdf["geometry"] = final_gdf.buffer(0)
            
        # Fix Date Columns
        for col in final_gdf.select_dtypes(include=['datetime64[ns]', 'datetime64[ns, UTC]']).columns:
            final_gdf[col] = final_gdf[col].apply(lambda v: v.date() if pd.notna(v) and hasattr(v, 'date') else v)
        
        # Truncate Column Names (Shapefile limit 10 chars)
        cleaned_truncated_columns = [col.lower().replace("__", "_")[:10] for col in final_gdf.columns]
        final_gdf.columns = cleaned_truncated_columns

        # De-duplicate columns
        final_cols = []
        col_counts = {}
        for col in final_gdf.columns:
            current_name = col
            if current_name in col_counts:
                col_counts[current_name] += 1
                suffix = str(col_counts[current_name])
                base_len = 10 - len(suffix) -1
                if base_len < 1:
                    current_name = col[:10-len(suffix)] + suffix
                else:
                    current_name = col[:base_len] + suffix
            else:
                col_counts[current_name] = 0
            final_cols.append(current_name)
        final_gdf.columns = final_cols

        final_gdf.to_file(out_path, driver="ESRI Shapefile", encoding='utf-8')
        print(f"✅ Saved: {out_path}")
        return out_path
        
    except Exception as e:
        print(f"❌ Save Failed for {layer_name}: {e}")
        return None

def get_urls(city, county):
    if not city or not county: return {}
    
    city = city.lower()
    county = county.lower()

    if "wake" in county:
        if "raleigh" in city:
            return {
                'topo': "https://maps.raleighnc.gov/hosted/rest/services/Hosted/Raleigh_Topography/FeatureServer/0/query",
                'parcels': "https://maps.wake.gov/arcgis/rest/services/Property/Parcels/FeatureServer/0/query",
                'roads': "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Roads/FeatureServer/0/query",
                'buildings': "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Building_Footprints/FeatureServer/0/query",
                'parking': "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Parking_Areas/FeatureServer/0/query",
                'vegetation': "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Vegetation_Outlines/FeatureServer/0/query",
                'stream': "https://services1.arcgis.com/a7CWfuGP5ZnLYE7I/arcgis/rest/services/USGSBlueLineStreams/FeatureServer/0/query",
            }
        elif "cary" in city:
            return {
                'topo': "https://maps.townofcary.org/arcgis/rest/services/Topography/Topography/MapServer/11/query",
            }
        else:
            return {
                'parcels': "https://maps.wake.gov/arcgis/rest/services/Property/Parcels/FeatureServer/0/query",
                'stream': "https://services1.arcgis.com/a7CWfuGP5ZnLYE7I/arcgis/rest/services/USGSBlueLineStreams/FeatureServer/0/query",
            }
    elif "durham" in county:
        return {
            'parcels': "https://gisweb.durhamnc.gov/arcgis/rest/services/Property/Parcels/FeatureServer/0/query",
            'stream': "https://services.arcgis.com/kTR3K8S9ke3XQfd6/arcgis/rest/services/Durham_Stream/FeatureServer/0/query",
        }
    elif "johnston" in county:
        return {
            'parcels': "https://gis.johnstonnc.com/arcgis/rest/services/Parcels/FeatureServer/0/query",
            'stream': "https://services1.arcgis.com/a7CWfuGP5ZnLYE7I/arcgis/rest/services/USGSBlueLineStreams/FeatureServer/0/query",
        }
    elif "nash" in county:
        return {
            'parcels': "https://maps.nashville.gov/arcgis/rest/services/Parcels/FeatureServer/0/query",
            'stream': "https://services1.arcgis.com/a7CWfuGP5ZnLYE7I/arcgis/rest/services/USGSBlueLineStreams/FeatureServer/0/query",
        }
    else:
        return {
            'stream': "https://services1.arcgis.com/a7CWfuGP5ZnLYE7I/arcgis/rest/services/USGSBlueLineStreams/FeatureServer/0/query"
        }

if __name__ == "__main__":
    suppress_warnings()
    
    # --- NON-BLOCKING INPUT HANDLING ---
    if len(sys.argv) >= 2:
        address = sys.argv[1]
    else:
        # Fallback for manual testing only
        print("Enter address as argument.")
        sys.exit(1)
        
    address += ", NC" 

    x, y, city, county = get_coords_nconemap(address)

    if x is None or y is None:
        print("❌ Address not found.")
        sys.exit(1) # Exit with error code for Java to catch
    
    if city is None or county is None:
        print("⚠️ City/County not identified.")
        sys.exit(0)

    print(f"📍 Location: {city.title()}, {county.title()}")
    print(f"✅ Coordinates: X={x}, Y={y}")
    
    # Check for "download layers" flag from Java
    download_map_layers = "n"
    if len(sys.argv) >= 3:
        download_map_layers = sys.argv[2] # "y" or "n" passed from Java

    shapefile_paths = []
    
    if download_map_layers.strip().lower().startswith('y'):
        urls = get_urls(city, county)
        if urls:
            for layer_name, url in urls.items():
                res = get_layer(x, y, layer_name, url)
                if res:
                    shapefile_paths.append(res)
        else:
            print("⚠️ No data sources for this county.")
    else:
        print("⚠️ Skipping layer downloads per request.")

    generate_script(x, y, shapefile_paths)

    print("DONE") # Signal to Java
    sys.exit(0)