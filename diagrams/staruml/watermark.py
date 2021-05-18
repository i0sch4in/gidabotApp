"""
This script removes UNREGISTERED watermark from a StarUML exported SVG
and converts it to PDF
"""
import lxml.etree as ET
from svglib.svglib import svg2rlg
from reportlab.graphics import renderPDF
import glob, os, shutil

os.chdir("./svg")
for file in glob.glob("*.svg"):
    output = file[:-4] + '.pdf'
    with open(file, 'r') as f:
        doc = ET.parse(f)
        for elem in doc.xpath("//*[local-name() = 'text' and text()='UNREGISTERED']"):
            if elem.attrib['fill'] == '#eeeeee' and elem.attrib['stroke'] == 'none' and elem.attrib['font-size'] == '24px':
                parent = elem.getparent()
                parent.remove(elem)
        tmp = ".tmp.svg"
        with open(tmp, 'wb') as f2:
            f2.write(ET.tostring(doc, pretty_print=True))
        drawing = svg2rlg(tmp)
        renderPDF.drawToFile(drawing, output)
        os.remove(tmp)
        shutil.move(output, "../pdf/" + output)
    
    print(output + " rendered.")
