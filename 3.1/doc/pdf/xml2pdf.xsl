<?xml version="1.0"?>

<xsl:stylesheet
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
     xmlns:fo="http://www.w3.org/1999/XSL/Format"
     xmlns:fox="http://xml.apache.org/fop/extensions">
<xsl:output indent="yes" encoding="UTF-8"/>

<xsl:variable name="indent">20pt</xsl:variable>
<xsl:variable name="images">images</xsl:variable>

<!-- title handling 
   -
   - param current should be a document, section, or s1 .. s6, or header element
   - param link: if given, is a file to get the header information
   - from if 'current' does not supply it.
  -->
<xsl:template name="m_title">
  <xsl:param name="current" select="."/>
  <xsl:param name="link"/>
  <xsl:param name="header">
    <xsl:copy-of select="if($current/header,$current/header/*,$current/*)"/>
    <xsl:if test="$link">
      <xsl:copy-of select="html_document($link)//header/*"/>
    </xsl:if>
  </xsl:param>

  <xsl:choose>
    <xsl:when test="$header/title">
      <xsl:apply-templates select="$header/title[1]/node()"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="if($current/title,$current/title[1],if($header[title],$header/title[1],$current/@title))"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!--
   - Top-level control
  -->
<xsl:template match="/">
  <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <!-- bookmarks -->

  <xsl:call-template name="bookmarks">
    <xsl:with-param name="list">
      <xsl:call-template name="toc">
        <xsl:with-param name="toc-depth" select="4"/>
        <xsl:with-param name="bookmarks" select="1"/>
      </xsl:call-template>
    </xsl:with-param>
  </xsl:call-template>


  <!-- defines page layout -->
  <fo:layout-master-set>
    <fo:simple-page-master master-name="simple"
                           page-height="11in" 
                           page-width="8.5in"
                           margin-top="0.25in" 
                           margin-bottom="0.25in" 
                           margin-left="1in" 
                           margin-right="1in">
      <fo:region-body margin-top="0.5in" margin-bottom="0.5in"/>
      <fo:region-before extent="0.5in"/>
      <fo:region-after extent="0.5in"/>
    </fo:simple-page-master>
  </fo:layout-master-set>

  <fo:page-sequence master-reference="simple">
    <fo:flow flow-name="xsl-region-body">
    <fo:block space-before="4in"
              font-size="40pt" 
              font-family="sans-serif" 
              line-height="50pt"
              space-after.optimum="25pt"
              font-weight="bold"
              text-align="center"
              start-indent="15pt">
      <xsl:value-of select="//book/@title"/>
    </fo:block>
    </fo:flow>
  </fo:page-sequence>

  <!-- table of contents -->

  <xsl:variable name="toc-depth" select="//book/@toc-depth"/>

  <xsl:if test="not($toc-depth = 0)">
    <fo:page-sequence master-reference="simple">
      <fo:static-content flow-name="xsl-region-after">
        <fo:block text-align="end" 
          font-size="10pt" 
          font-family="serif"
          text-align="center"
          line-height="14pt">
          - <fo:page-number/> -
        </fo:block>
      </fo:static-content> 

      <fo:flow flow-name="xsl-region-body">
        <fo:block font-size="18pt" 
          font-family="sans-serif" 
          line-height="24pt"
          space-after.optimum="25pt"
          font-weight="bold"
          text-align="center"
          start-indent="15pt">
          Table of Contents
        </fo:block>

        <fo:block>
          <xsl:call-template name="toc">
            <xsl:with-param name="toc-depth" select="if($toc-depth,$toc-depth,2)"/>
          </xsl:call-template>
        </fo:block>

      </fo:flow>
    </fo:page-sequence>
  </xsl:if>


  <!-- content -->
  <xsl:apply-templates/>

  </fo:root>

</xsl:template>

<xsl:template name="bookmarks">
  <xsl:param name="list"/>
  <xsl:param name="parent.id"/>

  <xsl:for-each select="$list/fox:outline[@parent.id=$parent.id]"> 
    <fox:outline internal-destination="{@internal-destination}">
      <fox:label><xsl:value-of select="fox:label"/></fox:label>
      <xsl:call-template name="bookmarks">
        <xsl:with-param name="list" select="$list"/>
        <xsl:with-param name="parent.id" select="@internal-destination"/>
      </xsl:call-template>
    </fox:outline>
  </xsl:for-each>
</xsl:template>

<!-- IGNORE and do children -->

<xsl:template match="book">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="html">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="body">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="th/@width">
  <xsl:apply-templates/>
</xsl:template>

<!-- old resin doc tags -->
<xsl:template match="eg-em">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="sum">
  <xsl:apply-templates/>
</xsl:template>

<!-- EAT -->
<xsl:template match="jsp:scriptlet">
  <!-- eat -->
</xsl:template>

<xsl:template match="header">
  <!-- eat -->
</xsl:template>

<xsl:template match="summary">
  <!-- eat -->
</xsl:template>

<xsl:template match="TODO">
  <!-- eat -->
</xsl:template>

<xsl:template match="form">
  <!-- eat -->
</xsl:template>

<xsl:template match="parents">
  <!-- eat, pulled in by deftable-childtags -->
</xsl:template>

<xsl:template match="default">
  <!-- eat, pulled in by deftable-childtags -->
</xsl:template>

<!-- table of contents -->

<xsl:template name="toc">
  <xsl:param name="bookmarks"/>

  <!-- If bookmarks is true, generate bookmarks instead of toc lines.
       The bookmarks are flat, so the caller needs to reorganize them 
       into a hierarchy -->

  <xsl:param name="toc-depth" select="2"/>


  <xsl:variable name="parent.id" select="''"/>

  <xsl:for-each select="//book/chapter | //book/document"> 
    <xsl:choose>
      <xsl:when test="$bookmarks">
        <fox:outline 
          internal-destination="{generate-id(.)}"
          parent.id="{$parent.id}">
          <fox:label><xsl:call-template name="m_title"/></fox:label>
        </fox:outline>
      </xsl:when>
      <xsl:otherwise>
        <fo:block text-align-last='justify'>
          <xsl:number level="multiple" format="1"/><xsl:text>) </xsl:text>

          <fo:basic-link color="blue">
            <xsl:attribute name="internal-destination">
              <xsl:value-of select="generate-id(.)"/>
            </xsl:attribute>
            <xsl:call-template name="m_title"/>
          </fo:basic-link>

          <xsl:text> </xsl:text>
          <fo:leader leader-pattern='rule' leader-alignment="page"/>
          <xsl:text> </xsl:text>

          <fo:page-number-citation ref-id="{generate-id(.)}"/>
        </fo:block>
      </xsl:otherwise>
    </xsl:choose>

    <!-- toc depth 2 -->
    <xsl:if test="$toc-depth &gt; 1">
      <xsl:variable name="parent.id" select="generate-id(.)"/>
      <xsl:for-each select="document|section|faq|body/section|body/faq">
        <xsl:choose>
          <xsl:when test="$bookmarks">
            <fox:outline 
              internal-destination="{generate-id(.)}"
              parent.id="{$parent.id}">
              <fox:label><xsl:call-template name="m_title"/></fox:label>
            </fox:outline>
          </xsl:when>
          <xsl:otherwise>
            <fo:block start-indent="10mm" text-align-last='justify'>
              <xsl:number level="multiple" count="chapter|document|section|faq" format="1.1"/>
              <xsl:text>) </xsl:text>

              <fo:basic-link color="blue">
                <xsl:attribute name="internal-destination">
                  <xsl:value-of select="generate-id(.)"/>
                </xsl:attribute>
                <xsl:call-template name="m_title"/>
              </fo:basic-link>

              <xsl:text> </xsl:text>
              <fo:leader leader-pattern='rule' leader-alignment="page"/>
              <xsl:text> </xsl:text>

              <fo:page-number-citation ref-id="{generate-id(.)}"/>
            </fo:block>
          </xsl:otherwise>
        </xsl:choose>


        <!-- toc depth 3 -->
        <xsl:if test="$toc-depth &gt; 2">
          <xsl:variable name="parent.id" select="generate-id(.)"/>
          <xsl:for-each select="document|section|faq|body/section|body/faq">
            <xsl:choose>
              <xsl:when test="$bookmarks">
                <fox:outline 
                  internal-destination="{generate-id(.)}"
                  parent.id="{$parent.id}">
                  <fox:label><xsl:call-template name="m_title"/></fox:label>
                </fox:outline>
              </xsl:when>
              <xsl:otherwise>
                <fo:block start-indent="20mm" text-align-last='justify'>
                  <xsl:number level="multiple" count="chapter|document|section|faq" format="1.1.1"/>
                  <xsl:text>) </xsl:text>

                  <fo:basic-link color="blue">
                    <xsl:attribute name="internal-destination">
                      <xsl:value-of select="generate-id(.)"/>
                    </xsl:attribute>
                    <xsl:call-template name="m_title"/>
                  </fo:basic-link>

                  <xsl:text> </xsl:text>
                  <fo:leader leader-pattern='rule' leader-alignment="page"/>
                  <xsl:text> </xsl:text>

                  <fo:page-number-citation ref-id="{generate-id(.)}"/>
                </fo:block>
              </xsl:otherwise>
            </xsl:choose>


            <!-- toc depth 4 -->
            <xsl:if test="$toc-depth &gt; 3">
              <xsl:variable name="parent.id" select="generate-id(.)"/>
              <xsl:for-each select="document|section|faq|body/section|body/faq">
                <xsl:choose>
                  <xsl:when test="$bookmarks">
                    <fox:outline 
                      internal-destination="{generate-id(.)}"
                      parent.id="{$parent.id}">
                      <fox:label><xsl:call-template name="m_title"/></fox:label>
                    </fox:outline>
                  </xsl:when>
                  <xsl:otherwise>
                    <fo:block start-indent="30mm" text-align-last='justify'>
                      <xsl:number level="multiple" count="chapter|document|section|faq" format="1.1.1.1"/>
                      <xsl:text>) </xsl:text>

                      <fo:basic-link color="blue">
                        <xsl:attribute name="internal-destination">
                          <xsl:value-of select="generate-id(.)"/>
                        </xsl:attribute>
                        <xsl:call-template name="m_title"/>
                      </fo:basic-link>

                      <xsl:text> </xsl:text>
                      <fo:leader leader-pattern='rule' leader-alignment="page"/>
                      <xsl:text> </xsl:text>

                      <fo:page-number-citation ref-id="{generate-id(.)}"/>
                    </fo:block>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </xsl:if> <!-- toc depth 4 -->
          </xsl:for-each>
        </xsl:if> <!-- toc depth 3 -->
      </xsl:for-each>
    </xsl:if> <!-- toc depth 2 -->
  </xsl:for-each>
</xsl:template>

<!--
   - chapter
   -->
<xsl:template match="chapter">
    <fo:page-sequence master-reference="simple">
      <fo:static-content flow-name="xsl-region-after">
        <fo:block text-align="end" 
          font-size="10pt" 
          font-family="serif"
          text-align="center"
          line-height="14pt">
          - <fo:page-number/> -
        </fo:block>
      </fo:static-content> 

      <fo:flow flow-name="xsl-region-body">
        <fo:block font-size="72pt" 
          font-family="sans-serif" 
          line-height="24pt"
          space-before.optimum="5in"
          space-after.optimum="25pt"
          font-weight="bold"
          text-align="center"
          start-indent="15pt">

          <xsl:attribute name="id">
            <xsl:value-of select="generate-id(.)"/>
          </xsl:attribute>

          <xsl:call-template name="m_title"/>
        </fo:block>

      </fo:flow>
    </fo:page-sequence>

    <xsl:apply-templates/>
</xsl:template>

<!--
   - section start's a new section.  You can have nested sections, their
   - titles are formatted appropriately.  
   -
   - note: <document/> is essentially just a outermost <section/>
  -->

<xsl:template match="section|document">
  <xsl:param name="section.depth" select="if(name()='section',1,0)"/>

  <xsl:variable name="depth" select="if(@depth,@depth,$section.depth + 1)"/>

  <xsl:choose>
    <xsl:when test="$depth = 1">
      <fo:page-sequence master-reference="simple">
        <fo:static-content flow-name="xsl-region-before">
          <fo:table width="100%" font-size="10pt" font-family="serif" table-layout="fixed">
            <fo:table-column/>
            <fo:table-column/>
            <fo:table-body>
              <fo:table-row>
                <fo:table-cell text-align="left">
                  <fo:block><xsl:value-of select="../@title"/></fo:block>
                </fo:table-cell>
                <fo:table-cell text-align="right">
                  <fo:block text-align="right">
                    <xsl:value-of select="@title"/> - p. <fo:page-number/>
                  </fo:block>
                </fo:table-cell>
              </fo:table-row>
            </fo:table-body>
          </fo:table>
        </fo:static-content> 

        <fo:static-content flow-name="xsl-region-after">
          <fo:block text-align="center" font-style="italic" font-size="8pt">Copyright (c) 1998-2004 Caucho Technology.  All rights reserved</fo:block>
          <fo:block font-size="10pt" 
            font-family="serif"
            text-align="center"
            line-height="14pt">
            - <fo:page-number/> -
          </fo:block>
        </fo:static-content> 

        <fo:flow flow-name="xsl-region-body"
          font-size="11pt" 
          font-family="sans-serif" 
          line-height="13pt">
          <fo:block font-size="18pt" 
            font-family="sans-serif" 
            line-height="24pt"
            space-before.optimum="15pt"
            space-after.optimum="15pt"
            background-color="blue"
            color="white"
            text-align="center">
            <xsl:attribute name="id">
              <xsl:value-of select="generate-id(.)"/>
            </xsl:attribute>
            <xsl:call-template name="m_title"/>
          </fo:block>

          <xsl:apply-templates>
            <xsl:with-param name="section.depth" select="$depth"/>
          </xsl:apply-templates>

        </fo:flow>
      </fo:page-sequence>
    </xsl:when>

    <xsl:when test="$depth = 2">
      <fo:block font-size="16pt" 
        font-family="sans-serif" 
        line-height="20pt"
        space-before.optimum="15pt"
        space-after.optimum="12pt"
        keep-with-previous="0"
        keep-with-next="always"
        padding-top="3pt">
        <xsl:attribute name="id">
          <xsl:value-of select="generate-id(.)"/>
        </xsl:attribute>
        <xsl:number level="multiple" count="s1|s2" format="1.1 "/>

        <xsl:value-of select="@title"/>
      </fo:block>

      <xsl:apply-templates/> 
    </xsl:when>
    <xsl:otherwise>
      <fo:block font-size="14pt" 
        font-family="sans-serif" 
        line-height="18pt"
        space-before.optimum="10pt"
        space-after.optimum="9pt"
        text-align="start"
        padding-top="3pt"
        keep-with-previous="0"
        keep-with-next="always">
        <xsl:value-of select="@title"/>
      </fo:block>
      <xsl:apply-templates>
        <xsl:with-param name="section.depth" select="$depth"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- defun -->
<xsl:template match ="defun">
   <fo:block font-size="14pt" 
            font-family="sans-serif" 
            line-height="18pt"
            space-before.optimum="10pt"
            space-after.optimum="9pt"
            text-align="start"
            padding-top="3pt"
            keep-with-previous="0"
            keep-with-next="always">
     <xsl:attribute name="id">
       <xsl:value-of select="generate-id(.)"/>
     </xsl:attribute>
     <xsl:value-of select="@title"/>
   </fo:block>

   <xsl:if test="parents">
     <fo:block 
       margin-left="1cm">
       <fo:inline font-weight="bold">child of: </fo:inline>
       <xsl:value-of select="parents"/>
     </fo:block>
   </xsl:if>
   <xsl:if test="default">
     <fo:block 
       margin-left="1cm">
       <fo:inline font-weight="bold">default: </fo:inline>
       <xsl:value-of select="default"/>
     </fo:block>
   </xsl:if>
   <fo:block margin-left="1cm">
     <xsl:apply-templates/> 
   </fo:block>
</xsl:template>

<!-- faq -->
<xsl:template match ="faq">
   <fo:block font-size="14pt" 
            font-family="sans-serif" 
            line-height="18pt"
            space-before.optimum="10pt"
            space-after.optimum="9pt"
            text-align="start"
            padding-top="3pt"
            keep-with-previous="0"
            keep-with-next="always">
       <xsl:attribute name="id">
         <xsl:value-of select="generate-id(.)"/>
       </xsl:attribute>
     <xsl:value-of select="@title"/>
   </fo:block>
   <xsl:if test="description">
     <fo:block 
       margin-left="1cm"
       space-after.optimum="9pt"
       font-style="italic">
       <xsl:apply-templates select="description/text() | description/node()"/>
     </fo:block>
   </xsl:if>
   <fo:block>
     <xsl:apply-templates select="node()"/> 
   </fo:block>
</xsl:template>

<!-- website only -->
<xsl:template match="website">
</xsl:template>

<!-- website only summary -->
<xsl:template match="summarylist">
</xsl:template>

<!--
   - LISTS
  -->

<!-- ul (unordered list) -->
<xsl:template match ="ul">
  <fo:list-block start-indent="{if(parent::li,2,1)}cm"
                 provisional-label-separation="1pt"
                 line-height="14pt">
     <xsl:apply-templates/> 
  </fo:list-block>
</xsl:template>


<!-- ol (ordered list) -->
<xsl:template match="ol">
  <fo:list-block start-indent="{if(parent::li,2,1)}cm" 
                 provisional-label-separation="1pt"
                 line-height="14pt">
     <xsl:apply-templates/> 
   </fo:list-block>
</xsl:template>


<!-- li (list item) in unordered list -->
<xsl:template match ="ul/li">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block>&#x2022;</fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates/> 
       </fo:block>
      </fo:list-item-body>
    </fo:list-item>
</xsl:template>


<!-- li (list item) in ordered list -->
<xsl:template match ="ol/li">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block>
          <xsl:number format="1. "/>
        </fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates/> 
       </fo:block>
      </fo:list-item-body>
    </fo:list-item>
</xsl:template>

<!-- dl (definition list) -->
<xsl:template match ="dl">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match ="dt">
  <fo:block space-before.optimum="8pt" font-weight="bold">
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match ="dd">
  <fo:block 
    space-before.optimum="8pt" 
    font-weight="normal"
    margin-left="1cm">
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<!--
   - TABLES
  -->

<xsl:template match="table|deftable|deftable-childtags|deftable-parameters">
  <xsl:if test="@title|title">
     <fo:block text-align='center' font-size='14pt'
              space-before.optimum="15pt"
              keep-with-next="always">
      <xsl:call-template name="m_title"/>
    </fo:block>
  </xsl:if>

  <fo:table table-layout="fixed"
            start-indent="{$indent}"
            font-size="10pt"
            line-height="13pt"
            font-family="sans-serif"
            border="1pt"
            space-before.optimum="15pt"
            space-after.optimum="15pt"
            margin-left="1cm"
            border-collapse="collapse">
    <xsl:choose>
      <xsl:when test="name() = 'deftable-childtags'">
        <fo:table-column column-width="{6 div 3}in"/>
        <fo:table-column column-width="{6 div 3}in"/>
        <fo:table-column column-width="{6 div 3}in"/>
      </xsl:when>
      <xsl:when test="name() = 'deftable-parameters'">
        <fo:table-column column-width="{6 div 3}in"/>
        <fo:table-column column-width="{6 div 3}in"/>
        <fo:table-column column-width="{6 div 3}in"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="tr[1]/td|tr[1]/th">
          <fo:table-column column-width="{6 div last()}in"/>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
            
    <fo:table-body>
      <xsl:if test="name() = 'deftable-childtags'">
        <xsl:variable name="heading">
          <tr><th>Attribute</th><th>Meaning</th><th>default</th></tr>
        </xsl:variable>
        <xsl:apply-templates select="$heading"/>
      </xsl:if>
      <xsl:if test="name() = 'deftable-parameters'">
        <xsl:variable name="heading">
          <tr><th>Parameter</th><th>Meaning</th><th>default</th></tr>
        </xsl:variable>
        <xsl:apply-templates select="$heading"/>
      </xsl:if>

      <xsl:apply-templates select="tr"/>
    </fo:table-body>
  </fo:table>
</xsl:template>

<xsl:template match="tr">
  <fo:table-row>
    <xsl:apply-templates select="th|td"/>
  </fo:table-row>
</xsl:template>

<xsl:template match="th">
  <fo:table-cell
            border="1pt"
            border-style="solid">
    <fo:block>
      <xsl:apply-templates select="@*|node()"/>
    </fo:block>
  </fo:table-cell>
</xsl:template>

<xsl:template match="td">
  <fo:table-cell
            border="0.3pt"
            border-style="solid">
    <fo:block>
      <xsl:apply-templates select="@*|node()"/>
    </fo:block>
  </fo:table-cell>
</xsl:template>

<xsl:template match="@occur">
</xsl:template>

<!--
   - FIGURES
  -->

<xsl:template match="figure">
  <fo:block>
    <fo:external-graphic src="file:{$images}/{@src}"/>
  </fo:block>
</xsl:template>

<!--
   - BOXES
  -->

<xsl:template match="example|result|results|def">
    <xsl:if test="@title|title">
      <fo:block text-align='center' font-size='14pt'
                keep-with-next="always"
                space-before.optimum="15pt">
        <xsl:call-template name="m_title"/>
      </fo:block>
    </xsl:if>

    <fo:table start-indent="0.25in"
            table-layout="fixed"
            space-before.optimum="{if(not(@title|title) and (not(parent::example-box) or position() = 1), 15, 0)}pt"
            space-after.optimum="15pt"
            border-style="solid">
       <xsl:choose>
       <xsl:when test="@title|title">
       </xsl:when>
       <xsl:otherwise>
         <xsl:attribute name="space-before.optimum">15pt</xsl:attribute>
       </xsl:otherwise>
       </xsl:choose>
    <fo:table-column column-width="6in"/>
    <fo:table-body>
    <fo:table-row>
      <fo:table-cell font-size="10pt" font-family="Courier" border="1pt">
        <fo:block white-space-collapse="false" keep-together="always">
          <xsl:apply-templates select="node()"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
    </fo:table-body>
  </fo:table>
</xsl:template>

<xsl:template match="example-box">
  <xsl:apply-templates select="example|result"/>
</xsl:template>

<!--
   - glossary
   -
   - @type one of 'inline', 'sidebar' (default), 'sidebar-left'
  -->
<xsl:template match="glossary">
    <xsl:if test="@title|title">
      <fo:block text-align='center' font-size='14pt'
                keep-with-next="always"
                space-before.optimum="15pt">
        <xsl:call-template name="m_title"/>
      </fo:block>
    </xsl:if>

    <xsl:if test="@type != 'inline'">
      <xsl:comment>transformError: glossary type `<xsl:value-of select="@type"/>' not impl for pdf, treating as inline</xsl:comment>
    </xsl:if>
    
    <fo:table start-indent="0.25in"
            table-layout="fixed"
            space-before.optimum="{if(not(@title|title) and (not(parent::example-box) or position() = 1), 15, 0)}pt"
            space-after.optimum="15pt"
            border-style="solid">
       <xsl:choose>
       <xsl:when test="@title|title">
       </xsl:when>
       <xsl:otherwise>
         <xsl:attribute name="space-before.optimum">15pt</xsl:attribute>
       </xsl:otherwise>
       </xsl:choose>
    <fo:table-column column-width="6in"/>
    <fo:table-body>
    <fo:table-row>
      <fo:table-cell border="1pt">
        <fo:block white-space-collapse="false" keep-together="always">
          <xsl:apply-templates select="node()"/>
        </fo:block>
      </fo:table-cell>
    </fo:table-row>
    </fo:table-body>
  </fo:table>
</xsl:template>

<!--
   - PARAGRAPH TEXT STYLES
  -->

<!-- p -->
<xsl:template match ="p">
   <fo:block space-after="3pt" space-before="3pt">
     <xsl:apply-templates/> 
   </fo:block>
</xsl:template>

<!-- sidebar -->
<xsl:template match ="sidebar">
   <fo:block space-after="3pt" space-before="3pt">
     <xsl:apply-templates/> 
   </fo:block>
</xsl:template>

<!-- note -->
<xsl:template match ="note"> 
   <fo:block space-after="3pt" space-before="3pt">
     <fo:inline font-weight="bold">Note: </fo:inline>
     <xsl:apply-templates/> 
   </fo:block>
</xsl:template>

<!-- warn -->
<xsl:template match ="warn"> 
   <fo:block color="#ff0055" space-after="3pt" space-before="3pt">
     <xsl:apply-templates/> 
   </fo:block>
</xsl:template>

<!--
   - INLINE TEXT STYLES
  -->

<!-- code -->
<xsl:template match ="code">
  <fo:wrapper font-family="Courier">
    <xsl:apply-templates/> 
  </fo:wrapper>
</xsl:template>

<xsl:template match="var">
   <fo:wrapper font-style="italic">
     <xsl:apply-templates/> 
   </fo:wrapper>
</xsl:template>

<xsl:template match="em">
   <fo:wrapper font-style="italic">
     <xsl:apply-templates/> 
   </fo:wrapper>
</xsl:template>

<xsl:template match="i">
   <fo:wrapper font-style="italic">
     <xsl:apply-templates/> 
   </fo:wrapper>
</xsl:template>

<xsl:template match="b">
   <fo:wrapper font-weight="bold">
     <xsl:apply-templates/> 
   </fo:wrapper>
</xsl:template>

<xsl:template match="bold">
   <fo:wrapper font-weight="bold">
     <xsl:apply-templates/> 
   </fo:wrapper>
</xsl:template>

<!--
   - links
  -->
<!-- jump (links) -->
<!--
<xsl:template match ="jump|a">
   <fo:basic-link color="blue" external-destination="{@href}">
     <xsl:apply-templates/> 
   </fo:basic-link>
</xsl:template>
  -->

<xsl:template match="a">
  <xsl:choose>
    <xsl:when test="@config-tag">
      <fo:wrapper font-family="Courier">
        <xsl:choose>
          <xsl:when test="node()|./text()">
            <xsl:apply-templates/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>&lt;</xsl:text>
            <xsl:value-of select="@config-tag"/>
            <xsl:text>&gt;</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </fo:wrapper>
    </xsl:when>
    <xsl:when test="node()|text()">
      <xsl:apply-templates/> 
    </xsl:when>
    <xsl:when test="@href">
     <xsl:choose>
        <xsl:when test="starts-with(@href,'javadoc|')">
          <xsl:variable name="s" select="substring-after(@href,'|')"/>
          <xsl:variable name="before" select="substring-before($s,'|')"/>
          <xsl:variable name="method" select="substring-after($s,'|')"/>
          <fo:wrapper font-family="Courier">
            <xsl:value-of select="if($before,$before,$s)"/>
            <xsl:if test="$method">
              <xsl:value-of select="concat('.',$method)"/>
            </xsl:if>
          </fo:wrapper>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@href"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:comment>transformError: link</xsl:comment>
    </xsl:otherwise>
  </xsl:choose>
  
</xsl:template>

<!--
  - MISC
  -->

<xsl:template match="resin">
  <xsl:text>Resin</xsl:text>
</xsl:template>

<!--
  - DEBUG
  -->

<xsl:template match="*|@*">
  <xsl:comment>transformError: unknown ele `<xsl:value-of select="name()"/>'</xsl:comment>
</xsl:template>


<!-- end body -->

</xsl:stylesheet>
