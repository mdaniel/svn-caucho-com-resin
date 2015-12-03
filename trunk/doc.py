#! /usr/bin/env python

import argparse as ap
import os
import sys
import re
from string import join
from glob import glob


#re_title_element = re.compile(r'<title>(.*)</title>')
#re_description = re.compile(r'<description>(.*)</description>')
#re_product = re.compile(r'<description>(.*)</description>')

re_title_attribute = re.compile(r'title=["|\'](.*)["|\']')
re_viewfile_link = re.compile(r'<viewfile-link\s+file=["|\'](.*)["|\']\s*/>')
re_link = re.compile(r'<a\s+href=["|\'](.*)["|\']\s*>(.*)</a>')









def die (msg, code) :
	print(msg)
	sys.exit(code)


def read_tag(line, f, tag):
	content = " "	 
	endtag = "</%s>" % tag
	starttag = "<%s>" % tag
	
	if endtag in line:
		content = line.replace(endtag, " ") 
		content = content.replace(starttag, " ")
		return content.strip()
	while line:
		if endtag in line:
			break
		line = f.readline()
		if endtag in line:
			break

		content = content + line


	return content

def read_title(line):

	if not "title" in line:
		return ""
	m = re_title_attribute.search(line)
	if not m:
		return ""
	title = m.group(1)
	return title

def replace_line(line):
	line = line.replace("<var>", "'''''")
	line = line.replace("</var>", "'''''")
	line = line.replace("<deftable>", "<table>")
	line = line.replace("</deftable>", "</table>")
	if "<viewfile-link" in line:
		m = re_viewfile_link.search(line)
		if m:
			file_name=m.group(1)
			line = line.replace(m.group(0),"<code>%s</code>" % file_name)
	if "<a" in line:
		m = re_link.search(line)
		if m:
			href=m.group(1)
			body=m.group(2)
			line = line.replace(m.group(0),"[%s %s]" % (href,body))

	return line

def example_extract(line, content, f):
	if "<example" in line: 
		title = read_title(line)
		if verbose: print "in example " + title
		content = content + "====%s====\n" % title
		content = content + "<pre>\n"
		while line:
			line = f.readline()
			if "</example>" in line:
				content = content + "</pre>\n"
				return f.readline(), content

			line = line.replace("&lt;", "<")
			line = line.replace("&gt;", ">")
			line = line.replace("</pre>", "&lt;/pre>")

			content = content + line
		if verbose: print "done with example " + title
	else:
		return line, content

def result_extract(line, content, f):
	if "<results" in line: 
		if verbose: print "in results "
		content = content + "====%s====\n" % "results"
		content = content + "<pre>\n"
		while line:
			line = f.readline()
			if "</results>" in line:
				content = content + "</pre>\n"
				return f.readline(), content
			content = content + line
	else: return line, content

class Document:
	def read_file(self, line, f):
		if verbose: print "read document"
		while line:
			line = f.readline()
			if "<header>" in line: 
				if verbose: print "read header"
				self.header = Header()
				self.header.read_file(line, f)
			elif "<body>" in line:
				if verbose: print "read body"
				self.body = Body()
				self.body.read_file(line, f)
				
class Header:
	def __init__(self):
		self.description=None
	def read_file(self, line, f):
		#print "read header"
		while line:
			line = f.readline()
			#print "header out", line
			if "<title>" in line: 
				self.title = read_tag(line, f, "title")
			elif "<description>" in line: 
				self.description = read_tag(line, f, "description")
			elif "</header>" in line:
				break
		#print "last line", line
		#print "description", self.description


class Body:
	def read_file(self, line, f):
		sections = []
		while line:
			line = f.readline()
			line = replace_line(line)
			if "<s1" in line: 
				if verbose: print "reading section 1"
				section = Section()
				section.read_file(line, f)
				sections.append(section)
				if verbose: "done reading section 1"
			if "<summary>" in line: 
				if verbose: print "reading summary"
				self.summary = read_tag(line, f, "summary")
				if verbose: print "done reading summary"
			if "</body>" in line:
				break
		self.sections = sections
		

class Section:
	def read_file(self, line, f):
		self.title = read_title(line)
		if verbose: print "section title " + self.title
		content = ""
		while line:
			line = f.readline()
			line = replace_line(line)


			line,content = example_extract(line, content, f)
			#print ("is tuple example", type(line))
			line,content = result_extract(line, content,f)
			#print ("is tuple results", type(line))
			

			if "<s2" in line or "<s3" in line: 
				title = read_title(line)
				if "<s2" in line:
					content = content + "===%s===\n" % title
				if "<s3" in line:
					content = content + "====%s====\n" % title
				content = content + "\n"

				while line:

					line = f.readline()
					line = replace_line(line)					
					line,content = example_extract(line, content, f)
					#print ("1", type(line))
					line,content = result_extract(line, content,f)
					#print ("2", type(line))
					if "</s2>" in line:
						break
					if "</s3>" in line:
						break
					
					content = content + line

			elif "</s1>" in line:
				break
			else:
				content = content + line

		self.content = content



class FileProcessor:		
	def __init__(self, args):
		self.verbose = args.verbose
		self.files = args.files
		if args.output:
			self.output = args.output

		
	def create_wiki_pages (self):
		if self.verbose: print ('files', self.files)
		for f in self.files:
			files = glob(f)
			for myfile in files:
				self.create_wiki_page(myfile)
		



		
	def create_wiki_page (self, file):
		if self.verbose: print ('file', file)
		if not os.path.exists(file):
			die ("%s file does not exist" % file, 6)		
		f = open (file, 'r')
		
		line = "used to read line by line from the file"
		try:
			
			while line:
				line = f.readline()
				#print (line)
				if "<document>" in line: 
					doc = Document()
					doc.read_file(line, f)


				


				
		except IOError as e: 
   			print "I/O error({0}): {1}".format(e.errno, e.strerror)
		finally:
			f.close()

		global outputfile
		if self.output:
			if not outputfile:
				outputfile = open(self.output, 'w')

		def printit(it):
			global outputfile
			if self.output:
				outputfile.write(it + "\n")
			else:
				print (it)

		if verbose: print ("\n" * 5)
		if verbose: print ("DOC:", doc.header.title, file)

		printit ("=%s=" % doc.header.title)
		if doc.header.description:
			printit(doc.header.description)
		for section in doc.body.sections:
			if section.title:
				printit("==%s==" % section.title)
			printit (section.content)
			
		
	

verbose = False
outputfile = None

def create_wiki_pages(args) :
	global verbose
	verbose = args.verbose
	fp = FileProcessor(args)
	fp.create_wiki_pages()
	

arg_parser = ap.ArgumentParser(description="Convert an xtp into a wiki page", epilog= 
"""Generates files for Eclipse and Wiki.""")

arg_parser.add_argument('files', metavar='FILES', nargs="+",
						help="Files to process")

arg_parser.add_argument('--verbose', '-v', dest='verbose', action='store_true',
                       help='Verbose mode')
						
arg_parser.add_argument('--wiki', '-w', dest='action', action='store_const',
                       const=create_wiki_pages, default=create_wiki_pages,
                       help='Create a wiki page based on a xtp test')

arg_parser.add_argument('--output', '-o', dest='output', action='store')

args = arg_parser.parse_args()


args.action(args)

if outputfile:
	print "Done"
	outputfile.close()

