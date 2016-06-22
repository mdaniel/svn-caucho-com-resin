<?php

/**
 * XML2Array: A class to convert XML to array in PHP
 * It returns the array which can be converted back to XML using the Array2XML script
 * It takes an XML string or a DOMDocument object as an input.
 *
 * See Array2XML: http://www.lalit.org/lab/convert-php-array-to-xml-with-attributes
 *
 * Author : Lalit Patel
 * Website: http://www.lalit.org/lab/convert-xml-to-array-in-php-xml2array
 * License: Apache License 2.0
 *          http://www.apache.org/licenses/LICENSE-2.0
 * Version: 0.1 (07 Dec 2011)
 * Version: 0.2 (04 Mar 2012)
 * 			Fixed typo 'DomDocument' to 'DOMDocument'
 *
 * Usage:
 *       $array = XML2Array::createArray($xml);
 */

class XML2Array {

	private static $xml = null;
	private static $encoding = 'UTF-8';

	/**
	 * Initialize the root XML node [optional]
	 * @param $version
	 * @param $encoding
	 * @param $format_output
	 */
	public static function init($version = '1.0', $encoding = 'UTF-8', $format_output = true) {
		self::$xml = new DOMDocument($version, $encoding);
		self::$xml->formatOutput = $format_output;
		self::$encoding = $encoding;
	}

	/**
	 * Convert an XML to Array
	 * @param string $node_name - name of the root node to be converted
	 * @param array $arr - aray to be converterd
	 * @return DOMDocument
	 */
	public static function &createArray($input_xml) {
		$xml = self::getXMLRoot();
		if(is_string($input_xml)) {
			$parsed = $xml->loadXML($input_xml);
			if(!$parsed) {
				throw new Exception('[XML2Array] Error parsing the XML string.');
			}
		} else {
			if(get_class($input_xml) != 'DOMDocument') {
				throw new Exception('[XML2Array] The input XML object should be of type: DOMDocument.');
			}
			$xml = self::$xml = $input_xml;
		}
		$array[$xml->documentElement->tagName] = self::convert($xml->documentElement);
		self::$xml = null;    // clear the xml node in the class for 2nd time use.
		return $array;
	}

	/**
	 * Convert an Array to XML
	 * @param mixed $node - XML as a string or as an object of DOMDocument
	 * @return mixed
	 */
	private static function &convert($node) {
		$output = array();

		switch ($node->nodeType) {
			case XML_CDATA_SECTION_NODE:
				$output['@cdata'] = trim($node->textContent);
				break;

			case XML_TEXT_NODE:
				$output = trim($node->textContent);
				break;

			case XML_ELEMENT_NODE:

				// for each child node, call the covert function recursively
				for ($i=0, $m=$node->childNodes->length; $i<$m; $i++) {
					$child = $node->childNodes->item($i);
					$v = self::convert($child);
					if(isset($child->tagName)) {
						$t = $child->tagName;

						// assume more nodes of same kind are coming
						if(!isset($output[$t])) {
							$output[$t] = array();
						}
						$output[$t][] = $v;
					} else {
						//check if it is not an empty text node
						if($v !== '') {
							$output = $v;
						}
					}
				}

				if(is_array($output)) {
					// if only one node of its kind, assign it directly instead if array($value);
					foreach ($output as $t => $v) {
						if(is_array($v) && count($v)==1) {
							$output[$t] = $v[0];
						}
					}
					if(empty($output)) {
						//for empty nodes
						$output = '';
					}
				}

				// loop through the attributes and collect them
				if($node->attributes->length) {
					$a = array();
					foreach($node->attributes as $attrName => $attrNode) {
						$a[$attrName] = (string) $attrNode->value;
					}
					// if its an leaf node, store the value in @value instead of directly storing it.
					if(!is_array($output)) {
						$output = array('@value' => $output);
					}
					$output['@attributes'] = $a;
				}
				break;
		}
		return $output;
	}

	/*
	 * Get the root XML node, if there isn't one, create it.
	 */
	private static function getXMLRoot(){
		if(empty(self::$xml)) {
			self::init();
		}
		return self::$xml;
	}
}

$xml = <<<EOT
<?xml version="1.0" encoding="UTF-8"?>
<movies type="documentary">
  <movie>
    <title>PHP: Behind the Parser</title>
    <characters>
      <character>
        <name>Ms. Coder</name>
        <actor>Onlivia Actora</actor>
      </character>
      <character>
        <name>Mr. Coder</name>
        <actor>El ActÓr</actor>
      </character>
    </characters>
    <plot><![CDATA[So, this language. It's like, a programming language. Or is it a scripting language? 
All is revealed in this thrilling horror spoof of a documentary.]]></plot>
    <great-lines>
      <line>PHP solves all my web problems</line>
    </great-lines>
    <rating type="thumbs">7</rating>
    <rating type="stars">5</rating>
  </movie>
</movies>
EOT;
$array = XML2Array::createArray($xml);
assert(isset($array['movies']['@attributes']['type']) && $array['movies']['@attributes']['type'] == 'documentary', 'Attributes were not extracted correctly');
assert(isset($array['movies']['movie']['characters']['character'][1]['name']) && $array['movies']['movie']['characters']['character'][1]['name'] == 'Mr. Coder', 'XML was not parsed correctly');

$xml = <<<EOT
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:i18n="http://www.w3.org/2005/09/ws-i18n" xmlns:typ="http://bees.com/determinations/server/interview/10.4/Honeybees/types">

<SOAP-ENV:Header>

<i18n:international>

<i18n:locale>en_GB</i18n:locale>

<i18n:tz>GMT-04:00</i18n:tz>

</i18n:international>

</SOAP-ENV:Header>

<SOAP-ENV:Body>

<typ:list-screens-response>

<typ:interview-session-id>d9ded305-64df-45b1-8fa8-7b05ddca2004</typ:interview-session-id>

<typ:entity id="global">

<typ:instance id="global">

<typ:screen id="qs" name="summary" title="Honeybee Registration Form" context-entity-id="global" context-instance-id="global" type="summary" is-automatic="false"/>

<typ:screen id="qs@Interviews_Honeybeescreens_xint" name="s4@Interviews_Honeybeescreens_xint" title="Information on Beeyards" context-entity-id="global" context-instance-id="global" type="question" is-automatic="false"/>

<typ:screen id="qs@Interviews_Honeybeescreens_xint" name="s10@Interviews_Honeybeescreens_xint" title="Personal Details Release" context-entity-id="global" context-instance-id="global" type="question" is-automatic="false"/>

<typ:screen id="qs@Interviews_Honeybeescreens_xint" name="s11@Interviews_Honeybeescreens_xint" title="Beekeeper Business Information" context-entity-id="global" context-instance-id="global" type="question" is-automatic="false"/>

<typ:screen id="qs@Interviews_Honeybeescreens_xint" name="s13@Interviews_Honeybeescreens_xint" title="Registration Complete" context-entity-id="global" context-instance-id="global" type="question" is-automatic="false"/>

<typ:screen id="qs@Interviews_Portalcommonscreens_xint" name="s2@Interviews_Portalcommonscreens_xint" title="Business Information - Contact Details" context-entity-id="global" context-instance-id="global" type="question" is-automatic="false"/>

<typ:screen id="qs@Interviews_Portalcommonscreens_xint" name="s4@Interviews_Portalcommonscreens_xint" title="Business Information - Address Details" context-entity-id="global" context-instance-id="global" type="question" is-automatic="false"/>

<typ:screen id="dr" name="Default Data Review" title="Data Review" context-entity-id="global" context-instance-id="global" type="data-review" is-automatic="false"/>

</typ:instance>

</typ:entity>

</typ:list-screens-response>

</SOAP-ENV:Body>

</SOAP-ENV:Envelope>


EOT;
$array = XML2Array::createArray($xml);
assert(isset($array['SOAP-ENV:Envelope']['SOAP-ENV:Body']['typ:list-screens-response']['typ:entity']['typ:instance']['typ:screen'][0]['@attributes']['title']) && $array['SOAP-ENV:Envelope']['SOAP-ENV:Body']['typ:list-screens-response']['typ:entity']['typ:instance']['typ:screen'][0]['@attributes']['title'] == 'Honeybee Registration Form', 'Attributes were not extracted correctly');
