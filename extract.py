import os
import re

def main():
    filepath = 'app/src/main/java/com/example/vapestoreapp/MainActivity.kt'
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the package declaration
    package_match = re.search(r'package\s+([a-zA-Z0-9_.]+)', content)
    base_package = package_match.group(1) if package_match else 'com.example.vapestoreapp'

    # Find all imports
    imports = re.findall(r'^import\s+.*$', content, re.MULTILINE)
    imports_str = '\n'.join(sorted(set(imports)))

    lines = content.split('\n')
    
    functions = {}
    main_activity_lines = []
    
    i = 0
    in_main_activity = False
    main_activity_brace_count = 0
    
    while i < len(lines):
        line = lines[i]
        
        if 'class MainActivity' in line:
            in_main_activity = True
            main_activity_lines.append(line)
            main_activity_brace_count += line.count('{') - line.count('}')
            i += 1
            continue
            
        if in_main_activity:
            main_activity_lines.append(line)
            main_activity_brace_count += line.count('{') - line.count('}')
            if main_activity_brace_count <= 0:
                in_main_activity = False
            i += 1
            continue
            
        # Check if it's a top level function
        is_annotation = line.strip().startswith('@')
        is_fun = line.strip().startswith('fun ')
        
        if is_annotation or is_fun:
            # Look ahead to see if it's a function
            start_idx = i
            j = i
            while j < len(lines) and lines[j].strip().startswith('@'):
                j += 1
            
            if j < len(lines) and lines[j].strip().startswith('fun '):
                # It's a function
                func_name_match = re.search(r'fun\s+([A-Z][a-zA-Z0-9_]*)', lines[j])
                if func_name_match:
                    func_name = func_name_match.group(1)
                    
                    # Find the matching closing brace
                    brace_count = 0
                    in_string = False
                    escape = False
                    found_brace = False
                    
                    end_idx = start_idx
                    for k in range(start_idx, len(lines)):
                        for char in lines[k]:
                            if escape:
                                escape = False
                                continue
                            if char == '\\':
                                escape = True
                                continue
                            if char == '"':
                                in_string = not in_string
                                continue
                            if not in_string:
                                if char == '{':
                                    brace_count += 1
                                    found_brace = True
                                elif char == '}':
                                    brace_count -= 1
                                    
                        if found_brace and brace_count == 0:
                            end_idx = k
                            break
                    
                    if found_brace and brace_count == 0:
                        func_content = '\n'.join(lines[start_idx:end_idx+1])
                        functions[func_name] = func_content
                        i = end_idx + 1
                        continue
        
        i += 1

    screens = [
        'AcceptScreen', 'SellScreen', 'CabinetScreen', 'ManagementScreen', 
        'SettingsScreen', 'DebtsScreen', 'ReservationsScreen', 'StockScreen', 
        'SalesScreen', 'ProductsScreen', 'SalesManagementScreen'
    ]
    
    dialogs = [
        'DaySalesDetailsDialog', 'CustomPeriodDialog', 'AddEditProductDialog', 
        'StockFilterDialog', 'BulkPriceDialog', 'EditBrandDialog', 
        'DeleteSaleDialog', 'EditSaleDialog'
    ]
    
    components = [
        'ProductItem', 'CompactCabinetButton', 'InfoRow', 'CompactSalesDayCard', 
        'SalesDayCard', 'SaleDetailCard', 'CompactProductTableRow', 
        'SalesManagementTableHeader', 'SalesManagementTableRow', 'SalesTableHeader', 
        'SalesTableRow', 'ProductsTableHeader', 'ProductTableRow'
    ]
    
    base_dir = 'app/src/main/java/com/example/vapestoreapp'
    screens_dir = os.path.join(base_dir, 'ui', 'screens')
    components_dir = os.path.join(base_dir, 'ui', 'components')
    dialogs_dir = os.path.join(base_dir, 'ui', 'components', 'dialogs')
    
    os.makedirs(screens_dir, exist_ok=True)
    os.makedirs(components_dir, exist_ok=True)
    os.makedirs(dialogs_dir, exist_ok=True)
    
    def write_file(directory, pkg_suffix, func_name, content):
        file_path = os.path.join(directory, f"{func_name}.kt")
        pkg = f"{base_package}.{pkg_suffix}"
        
        extra_imports = [
            f"import {base_package}.ui.screens.*",
            f"import {base_package}.ui.components.*",
            f"import {base_package}.ui.components.dialogs.*",
            f"import {base_package}.*"
        ]
        
        full_content = f"package {pkg}\n\n{imports_str}\n" + '\n'.join(extra_imports) + f"\n\n{content}\n"
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(full_content)
            
    for name, content in functions.items():
        if name in screens:
            write_file(screens_dir, 'ui.screens', name, content)
        elif name in dialogs:
            write_file(dialogs_dir, 'ui.components.dialogs', name, content)
        elif name in components:
            write_file(components_dir, 'ui.components', name, content)
            
    main_activity_content = f"package {base_package}\n\n{imports_str}\n"
    main_activity_content += f"import {base_package}.ui.screens.*\n"
    main_activity_content += f"import {base_package}.ui.components.*\n"
    main_activity_content += f"import {base_package}.ui.components.dialogs.*\n\n"
    
    main_activity_content += '\n'.join(main_activity_lines) + "\n\n"
    
    if 'AppNavigation' in functions:
        main_activity_content += functions['AppNavigation'] + "\n\n"
        
    if 'AppPreview' in functions:
        main_activity_content += functions['AppPreview'] + "\n\n"
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(main_activity_content)
        
    print("Extraction complete!")

if __name__ == '__main__':
    main()
