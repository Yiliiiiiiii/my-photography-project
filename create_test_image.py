import struct

def create_jpeg_with_exif(filename):
    # Minimal JPEG structure
    soi = b'\xFF\xD8'
    eoi = b'\xFF\xD9'
    
    # App1 Marker (Exif)
    app1_marker = b'\xFF\xE1'
    
    # Exif Header
    exif_header = b'Exif\x00\x00'
    
    # TIFF Header (Little Endian)
    tiff_header = b'II\x2A\x00\x08\x00\x00\x00'
    
    # IFD0 (Number of entries = 1)
    ifd0_num = b'\x01\x00'
    
    # Tag: Make (0x010F), Type: ASCII (2), Count: 5, Value Offset: Next
    # We will put the Value data after the IFD Link
    tag_make = b'\x0F\x01\x02\x00\x05\x00\x00\x00' # Offset placeholder
    
    # Link to next IFD (0)
    next_ifd = b'\x00\x00\x00\x00'
    
    # Calculating offset to data:
    # Header(8) + Num(2) + Tag(12) + Next(4) = 26 bytes
    # So valid data starts at 8 + 26 = 34? No, offset is from start of TIFF header.
    # 2 + 12 + 4 = 18 bytes of IFD data
    # Offset = 8 (first IFD) + 18 = 26. 
    tag_make_offset = struct.pack('<I', 26) 
    
    tag_make_entry = b'\x0F\x01\x02\x00\x05\x00\x00\x00' + tag_make_offset
    
    make_value = b'Test\x00' # 5 bytes
    
    exif_data_block = tiff_header + ifd0_num + tag_make_entry + next_ifd + make_value
    
    app1_len = struct.pack('>H', 2 + len(exif_header) + len(exif_data_block))
    
    with open(filename, 'wb') as f:
        f.write(soi)
        f.write(app1_marker)
        f.write(app1_len)
        f.write(exif_header)
        f.write(exif_data_block)
        f.write(eoi)
        
    print(f"Created {filename}")

if __name__ == '__main__':
    create_jpeg_with_exif("src/test/resources/test_exif.jpg")
