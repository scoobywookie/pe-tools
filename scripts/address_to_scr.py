# Daniel Puckett & Joe Puckett | Place Engineering, PLLC
# v2.0.0 - 1/19/2026 (Java Integration + Robust GIS Fusion)
# This script is "Frozen" ready for PyInstaller (.exe conversion)
# Combines Java I/O structure with Production-Grade GIS Fetching

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from geopy.geocoders import Nominatim
import geopandas as gpd
import pandas as pd
import sys
import os
import io

# --- 1. DYNAMIC PATH SETUP (For Java/Exe Compatibility) ---
# Detect User Desktop automatically to avoid hardcoded "Daniel" paths
USER_DESKTOP = os.path.join(os.path.expanduser("~"), "Desktop")
OUTPUT_FOLDER = os.path.join(USER_DESKTOP, "CAD-IMPORTS")

# Ensure folder exists
if not os.path.exists(OUTPUT_FOLDER):
    try:
        os.makedirs(OUTPUT_FOLDER)
    except OSError:
        pass

# Reconfigure stdout for Java ProcessBuilder to read UTF-8 correctly
# ENABLE LINE BUFFERING (Fixes the "Wait until the end" lag)
sys.stdout.reconfigure(encoding='utf-8', line_buffering=True)

def suppress_warnings():
    import warnings
    warnings.filterwarnings("ignore")

# --- 2. ENGINEERING GRADE SESSION (Handles Connection Drops) ---
def get_session():
    session = requests.Session()
    # Retry 3 times on connection errors (Fixes 10054 errors on hosted servers)
    retries = Retry(total=3, backoff_factor=1, status_forcelist=[500, 502, 503, 504])
    adapter = HTTPAdapter(max_retries=retries)
    session.mount('http://', adapter)
    session.mount('https://', adapter)
    # Browser header to avoid 403 blocks
    session.headers.update({'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    return session

def get_coords_nconemap(address):
    geolocator = Nominatim(user_agent="city_county_lookup_combined")
    try:
        geo_result = geolocator.geocode(address, addressdetails=True)
    except:
        return None, None, None, None

    city = county = None
    if geo_result and 'address' in geo_result.raw:
        addr = geo_result.raw['address']
        city = addr.get('city') or addr.get('town') or addr.get('village')
        county = addr.get('county')

    url = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates"
    params = {"SingleLine": address, "f": "json", "outSR": "2264", "maxLocations": 1}

    session = get_session()
    try:
        response = session.get(url, params=params, timeout=10)
        data = response.json()
        if data['candidates']:
            location = data['candidates'][0]['location']
            return location['x'], location['y'], city, county
    except:
        pass
    return None, None, city, county

def get_layer(x, y, layer_name, urls_list):
    print(f"📦 Fetching layer: {layer_name.title()}")

    if isinstance(urls_list, str): urls_list = [urls_list]
    out_path = os.path.join(OUTPUT_FOLDER, f"{layer_name}.shp")
    session = get_session()

    # --- ACTIVE FAILOVER LOOP ---
    for url in urls_list:
        offset = 0
        limit = 2000
        all_gdfs = []
        failed_this_url = False

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
                response = session.get(url, params=params, timeout=30)

                if response.status_code != 200:
                    failed_this_url = True; break

                data = response.json()
                if "features" not in data or not data["features"]:
                    break

                # --- CRITICAL FIX: Manual EsriJSON -> GeoJSON Translation ---
                # This prevents the 'NoneType' crash and handles messy data
                clean_features = []
                for feat in data["features"]:
                    attrs = feat.get("attributes", {})
                    clean_attrs = {}
                    for k, v in attrs.items():
                        if k is None or k == "": clean_attrs["field"] = v
                        else: clean_attrs[str(k)] = v

                    geom = feat.get("geometry")
                    if geom:
                        if "paths" in geom:
                            geom["type"] = "MultiLineString"
                            geom["coordinates"] = geom.pop("paths")
                        elif "rings" in geom:
                            geom["type"] = "Polygon"
                            geom["coordinates"] = geom.pop("rings")
                        elif "x" in geom and "y" in geom:
                            geom["type"] = "Point"
                            geom["coordinates"] = [geom["x"], geom["y"]]

                    clean_features.append({
                        "type": "Feature", "geometry": geom, "properties": clean_attrs
                    })

                gdf = gpd.GeoDataFrame.from_features(clean_features, crs="EPSG:2264")
                if gdf.empty: break

                all_gdfs.append(gdf)
                count = len(gdf)

                # Java Console Progress Update
                if sum(len(g) for g in all_gdfs) > 0:
                    print(f"   ... {sum(len(g) for g in all_gdfs)} items found")

                if count < limit: break
                offset += limit

            except Exception:
                failed_this_url = True
                break

        if failed_this_url: continue
        if not all_gdfs: continue

        # Success! Process and Save
        final_gdf = pd.concat(all_gdfs, ignore_index=True)
        if not final_gdf.is_valid.all(): final_gdf["geometry"] = final_gdf.buffer(0)

        for col in final_gdf.select_dtypes(include=['datetime64[ns]', 'datetime64[ns, UTC]']).columns:
            final_gdf[col] = final_gdf[col].apply(lambda v: v.date() if pd.notna(v) and hasattr(v, 'date') else v)

        # Truncate & Dedup Columns
        final_gdf.columns = [(str(col).lower() if col is not None else "field").replace("__", "_")[:10] for col in final_gdf.columns]

        final_cols = []
        col_counts = {}
        for col in final_gdf.columns:
            if col in col_counts:
                col_counts[col] += 1
                suffix = str(col_counts[col])
                final_cols.append(col[:10-len(suffix)-1] + "_" + suffix)
            else:
                col_counts[col] = 0
                final_cols.append(col)
        final_gdf.columns = final_cols

        try:
            final_gdf.to_file(out_path, driver="ESRI Shapefile", encoding='utf-8')
            print(f"✅ Saved: {out_path}")
            return out_path
        except Exception as e:
            print(f"❌ Failed to save {layer_name}: {e}")
            return None

    print(f"⚠️ No features found for {layer_name}")
    return None

def get_urls(city, county):
    if not city or not county: return {}
    city = city.lower()
    county = county.lower()

    # --- HYBRID CONFIG (Updated for 2026 Reliability) ---

    parcels_urls = [
        "https://maps.wake.gov/arcgis/rest/services/Property/Parcels/FeatureServer/0/query",
        "https://services.nconemap.gov/secure/rest/services/NC1Map_Parcels/MapServer/0/query"
    ]

    roads_urls = [
        "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Roads/FeatureServer/0/query",
        "https://maps.wake.gov/arcgis/rest/services/Transportation/Streets/MapServer/0/query",
        "https://services.nconemap.gov/secure/rest/services/NC1Map_Transportation/MapServer/0/query"
    ]

    buildings_urls = [
        "https://maps.wake.gov/arcgis/rest/services/Property/BuildingFootprints/MapServer/0/query",
        "https://services.nconemap.gov/secure/rest/services/NC1Map_Buildings_2024/MapServer/0/query",
        "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Building_Footprints/FeatureServer/0/query"
    ]

    stream_urls = [
        "https://services1.arcgis.com/a7CWfuGP5ZnLYE7I/arcgis/rest/services/USGSBlueLineStreams/FeatureServer/0/query",
        "https://services.nconemap.gov/secure/rest/services/NC1Map_Hydrography/MapServer/1/query",
        "https://maps.wake.gov/arcgis/rest/services/Environmental/Hydrography/MapServer/0/query"
    ]

    parking_urls = [
        "https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Parking_Areas/FeatureServer/0/query",
        "https://maps.raleighnc.gov/arcgis/rest/services/Planning/Parking/MapServer/0/query"
    ]

    vegetation_urls = ["https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Vegetation_Outlines/FeatureServer/0/query"]

    # --- NEW LAYERS (Driveways + Sidewalks) ---
    sidewalks_urls = ["https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/City_Of_Raleigh_Sidewalks_14_view/FeatureServer/0/query"]
    driveways_urls = ["https://services.arcgis.com/v400IkDOw1ad7Yad/arcgis/rest/services/Miscellaneous_Planimetric_Features/FeatureServer/0/query"]

    topo_urls = []
    if "raleigh" in city:
        topo_urls.append("https://maps.raleighnc.gov/hosted/rest/services/Hosted/Raleigh_Topography/FeatureServer/0/query")

    return {
        'parcels': parcels_urls,
        'roads': roads_urls,
        'buildings': buildings_urls,
        'stream': stream_urls,
        'parking': parking_urls,
        'vegetation': vegetation_urls,
        'driveways': driveways_urls,
        'sidewalks': sidewalks_urls,
        'topo': topo_urls if topo_urls else None
    }

def generate_script(x, y, shapefile_paths):
    radius = 5000
    script_path = os.path.join(OUTPUT_FOLDER, "circle_layers.scr")
    # Dynamic IPF path assuming it lives in the same folder
    ipf_path = os.path.join(OUTPUT_FOLDER, "gis data.ipf")

    try:
        with open(script_path, "w") as f:
            f.write("CIRCLE\n")
            f.write(f"{x},{y}\n")
            f.write(f"{radius}\n")
            f.write(f"ZOOM\nC\n{x},{y}\n{2 * radius}\n")

            def write_import(path):
                f.write(f"-MAPIMPORT\nshp\n{path}\nyes\n")
                f.write(f"{ipf_path}\nproceed\n")

            topo_layer = next((s for s in shapefile_paths if "topo" in s.lower()), None)
            if topo_layer:
                write_import(topo_layer)
                f.write('(load "apply_topo_elevation.lsp") AssignTopoElevation\n')

            for shp in shapefile_paths:
                if "topo" not in shp.lower():
                    write_import(shp)

            f.write('(load "enable_linetype_generation.lsp") EnableLinetypeGeneration\n')

        print(f"📁 Script generated: {script_path}")
    except Exception as e:
        print(f"❌ Failed to write script: {e}")

if __name__ == "__main__":
    suppress_warnings()

    # --- JAVA / CLI INPUT HANDLING ---
    if len(sys.argv) >= 2:
        address = sys.argv[1]
    else:
        print("Enter address as argument.")
        sys.exit(1)

    address += ", NC"
    x, y, city, county = get_coords_nconemap(address)

    if x is None:
        print("❌ Address not found.")
        sys.exit(1)

    if not city or not county:
        print("⚠️ City/County not identified.")
        sys.exit(0)

    print(f"📍 Location: {city.title()}, {county.title()}")
    print(f"✅ Coordinates: X={x}, Y={y}")

    # Java passes "y", "true", or nothing
    dl_arg = "false"
    if len(sys.argv) >= 3:
        dl_arg = sys.argv[2].strip().lower()

    paths = []
    if dl_arg == "true" or dl_arg.startswith('y'):
        urls = get_urls(city, county)
        if not urls:
            print("⚠️ No data sources for this location.")
        else:
            for name, url_list in urls.items():
                if not url_list: continue
                shp = get_layer(x, y, name, url_list)
                if shp: paths.append(shp)
    else:
        print("⚠️ Skipping layer downloads per request.")

    generate_script(x, y, paths)

    print("DONE") # Signal for Java ProcessBuilder
    sys.exit(0)