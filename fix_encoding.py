import os
import glob

def fix_encoding(file_path):
    content = None
    original_encoding = None

    # Try detecting UTF-8 (with or without BOM)
    try:
        with open(file_path, 'r', encoding='utf-8-sig') as f:
            content = f.read()
        original_encoding = 'utf-8/utf-8-sig'
    except UnicodeDecodeError:
        # Try GBK (common in Windows CN environment)
        try:
            with open(file_path, 'r', encoding='gbk') as f:
                content = f.read()
            original_encoding = 'gbk'
        except UnicodeDecodeError:
            print(f"Skipping {file_path}: Unknown encoding")
            return

    if content is not None:
        # Write back as UTF-8 NO BOM
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"converted {file_path} from {original_encoding} to utf-8")

# Walk through all java files
target_dir = r"e:\VScode\web\2025.11.3\test\my-photography-project\src\main\java"
for root, dirs, files in os.walk(target_dir):
    for file in files:
        if file.endswith(".java"):
            fix_encoding(os.path.join(root, file))
