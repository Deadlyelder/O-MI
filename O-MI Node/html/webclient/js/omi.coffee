
# import WebOmi, add submodule
omiExt = (WebOmi) ->

  # Sub module for handling omi xml
  my = WebOmi.omi = {}


  # Generic xml string parser
  my.parseXml = (responseString) ->
    try
      xmlTree = new DOMParser().parseFromString responseString, 'application/xml'
    catch ex
      # parsererror or FIXME: unsupported?
      xmlTree = null
      WebOmi.debug "DOMParser xml parsererror or not supported!"
  
    # mozilla parsererror
    if xmlTree.firstElementChild.nodeName == "parsererror" or not xmlTree?
      WebOmi.debug "PARSE ERROR:"
      WebOmi.debug "in:", responseString
      WebOmi.debug "out:", xmlTree
      xmlTree = null

    xmlTree


  # XML Namespace URIs used in the client
  # (needed because of the use of default namespaces with XPaths)
  my.ns =
    omi : "omi.xsd"
    odf : "odf.xsd"
    xsi : "http://www.w3.org/2001/XMLSchema-instance"
    xs  : "http://www.w3.org/2001/XMLSchema-instance"

  # XML Namespace resolver, (defaults to odf)
  my.nsResolver = (name) ->
    my.ns[name] || my.ns.odf

  # Generic Xpath evaluator
  my.evaluateXPath = (elem, xpath) ->
    xpe = elem.ownerDocument || elem
    iter = xpe.evaluate(xpath, elem, my.nsResolver, 0, null)

    res while res = iter.iterateNext()

  # private; Create odf element with the right namespace
  # doc: xml document
  # elem: string, odf element name
  createOdf = (elem, doc) ->
    doc.createElementNS(my.ns.odf, elem)

  my.createOmi = (elem, doc) ->
    doc.createElementNS(my.ns.omi, elem)

  my.createOdfValue = (doc) ->
    createOdf "value", doc

  my.createOdfObjects = (doc) ->
    createOdf "Objects", doc

  my.createOdfObject = (doc, id) ->
    createdElem = createOdf "Object", doc

    idElem      = createOdf "id", doc
    textElem    = doc.createTextNode(id)

    idElem.appendChild textElem
    createdElem.appendChild idElem
    createdElem

  # Create omi element with the right namespace
  my.createOdfInfoItem = (doc, name) ->
    createdElem = createOdf "InfoItem", doc
    createdElem.setAttribute "name", name
    createdElem


  # Gets the id of xmlNode Object or name of InfoItem
  # xmlNode: XmlNode
  # return: Maybe String
  my.getOdfId = (xmlNode) ->
    switch xmlNode.nodeName
      when "Object"
        head = my.evaluateXPath(xmlNode, './odf:id')[0]
        if head? then head.textContent.trim() else null
      when "InfoItem"
        nameAttr = xmlNode.attributes.name
        if nameAttr? then nameAttr.value else null
      when "Objects" then "Objects"
      else null

  # Checks if odfNode has odf element child with id or name of odfId
  # odfId: String, object name or infoitem name
  # odfNode: XmlNode, parent of whose children are checked
  # return Maybe XmlNode
  my.getOdfChild = (odfId, odfNode) ->
    for child in odfNode.childNodes
      if my.getOdfId(child) == odfId
        return child
    return null

  my.hasOdfChildren = (odfNode) ->
    # Could also be made from getOdfChildren
    for child in odfNode.childNodes
      maybeId = my.getOdfId(child)
      if maybeId? && maybeId != ""
        return true
    return false


  WebOmi # export module

# extend WebOmi
window.WebOmi = omiExt(window.WebOmi || {})