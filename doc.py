#! /usr/bin/env python

import argparse as ap
import os
import sys
import re
from string import join
from glob import glob


re_title_element = re.compile(r'<title>(.*)</title>')
re_description = re.compile(r'<description>(.*)</description>')
re_product = re.compile(r'<description>(.*)</description>')

re_title_attribute = re.compile(r'title=["|\'](.*)["|\']')





def die (msg, code) :
	print(msg)
	sys.exit(code)


def read_tag(line, f, tag):
	content = " "	 
	endtag = "</%s>" % tag

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


class Document:
	def read_file(self, line, f):
		#print "read document"
		while line:
			line = f.readline()
			if "<header>" in line: 
				self.header = Header()
				self.header.read_file(line, f)
			elif "<body>" in line:
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
			if "<s1" in line: 
				section = Section()
				section.read_file(line, f)
				sections.append(section)
			if "<summary>" in line: 
				self.summary = read_tag(line, f, "summary")
			if "</body>" in line:
				break
		self.sections = sections
		



class Section:
	def read_file(self, line, f):
		self.title = read_title(line)
		content = ""
		while line:
			line = f.readline()
			#print "section out", line
			if "<example" in line: 
				title = read_title(line)
				content = content + "====%s====\n" % title
				content = content + "<pre>\n"
				while line:
					line = f.readline()
					if "</example>" in line:
						content = content + "</pre>\n"
						break
					line = line.replace("&lt;", "<")
					line = line.replace("&gt;", ">")
					content = content + line
			elif "<results" in line: 
				content = content + "<pre>\n"
				while line:
					line = f.readline()
					if "</results>" in line:
						content = content + "</pre>\n"
						break
					content = content + line
			elif "<s2" in line or "<s3" in line: 
				title = read_title(line)
				if "<s2" in line:
					content = content + "==%s==\n" % title
				if "<s3" in line:
					content = content + "===%s===\n" % title
				content = content + "\n"
				while line:
					line = f.readline()
					if "</s2>" in line or "</s3>" in line:
						content = content + "\n"
						break
					line = line.replace("<var>", "'''''")
					line = line.replace("</var>", "'''''")
					line = line.replace("<deftable>", "<table>")
					line = line.replace("</deftable>", "</table>")
					content = content + line

			elif "</s1>" in line:
				break
			else:
				line = line.replace("<deftable>", "<table>")
				line = line.replace("</deftable>", "</table>")
				content = content + line
		self.content = content



class FileProcessor:		
	def __init__(self, args):
		self.verbose = args.verbose
		self.files = args.files
		
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

		print doc.header.title
		if doc.header.description:
			print doc.header.description
		for section in doc.body.sections:
			if section.title:
				print "=%s=" % section.title
			print section.content
			
			


def create_wiki_pages(args) :
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

args = arg_parser.parse_args()


args.action(args)
