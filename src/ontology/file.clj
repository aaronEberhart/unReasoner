(ns ontology.file
	;(:require	[clojure.java.io :as io][util.msc :as util})
  (:use [slingshot.slingshot :only [throw+]])
	)

(defn- -ontologyFile
	"ontologyDocument := { prefixDeclaration } Ontology"
  ([ontology]
    (if (= (:type ontology) :ontology)
      ontology
      (throw+ {:type ::notOntology :ontology ontology})))
  ([prefixes ontology]
  	(if (= (:type prefixes) :prefixes)
      (if (= (:type ontology) :ontology)
        (assoc ontology :prefixes (:prefixes prefixes))
        (throw+ {:type ::notOntology :ontology ontology}))
      (throw+ {:type ::notPrefixes :prefixes prefixes}))))

(defn prefix [prefixName longIRI]
	"prefixDeclaration := 'Prefix' '(' prefixName '=' fullIRI ')'"
  (if (and (string? prefixName)(string? longIRI))
	 {:prefix prefixName :iri longIRI :type :prefix :innerType :prefix}
   (throw+ {:type ::notIRIs :prefixName prefixName :longIRI longIRI})))

(defn prefixes [prefixes]
  (if (every? (fn [x] (= (:type x) :prefix)) prefixes)
    {:prefixes prefixes :type :prefixes :innerType :prefixes}
    (throw+ {:type ::notPrefixes :prefixes prefixes})))

(defn- -ontology
	"Ontology := 'Ontology' '(' [ ontologyIRI [ versionIRI ] ] directlyImportsDocuments ontologyAnnotations axioms ')'"
  ([directImports ontologyAnnotations axioms]
    (if (or (= nil directImports)(= (:type directImports) :imports))
      (if (or (= nil ontologyAnnotations)(= (:type ontologyAnnotations) :annotations))
        (if (or (= nil axioms)(= (:type axioms) :axioms))
          {:ontologyIRI nil :versionIRI nil :axioms (if axioms (:axioms axioms) nil) :imports (if directImports (:imports directImports) nil) :annotations (if ontologyAnnotations (:annotations ontologyAnnotations) nil) :type :ontology}
          (throw+ {:type ::notaxioms :axioms axioms}))
        (throw+ {:type ::notontologyAnnotations :ontologyAnnotations ontologyAnnotations}))
      (throw+ {:type ::directImports :directImports directImports})))
  ([ontologyIRI directImports ontologyAnnotations axioms]
    (if (or (= nil directImports)(= (:type directImports) :imports))
      (if (or (= nil ontologyAnnotations)(= (:type ontologyAnnotations) :annotations))
        (if (or (= nil axioms)(= (:type axioms) :axioms))
          (if (= (:type ontologyIRI) :ontologyIRI)
            {:ontologyIRI ontologyIRI :versionIRI nil :axioms (if axioms (:axioms axioms) nil) :imports (if directImports (:imports directImports) nil) :annotations (if ontologyAnnotations (:annotations ontologyAnnotations) nil) :type :ontology}
            (throw+ {:type ::notontologyIRI :ontologyIRI ontologyIRI}))
          (throw+ {:type ::notaxioms :axioms axioms}))
        (throw+ {:type ::notontologyAnnotations :ontologyAnnotations ontologyAnnotations}))
      (throw+ {:type ::directImports :directImports directImports})))
  ([ontologyIRI versionIRI directImports ontologyAnnotations axioms]
    (if (or (= nil directImports)(= (:type directImports) :imports))
      (if (or (= nil ontologyAnnotations)(= (:type ontologyAnnotations) :annotations))
        (if (or (= nil axioms)(= (:type axioms) :axioms))
          (if (= (:type ontologyIRI) :ontologyIRI)
            (if (= (:type versionIRI) :versionIRI)
              {:ontologyIRI ontologyIRI :versionIRI versionIRI :axioms (if axioms (:axioms axioms) nil) :imports (if directImports (:imports directImports) nil) :annotations (if ontologyAnnotations (:annotations ontologyAnnotations) nil) :type :ontology}
              (throw+ {:type ::notversionIRI :versionIRI versionIRI}))
            (throw+ {:type ::notontologyIRI :ontologyIRI ontologyIRI}))
          (throw+ {:type ::notaxioms :axioms axioms}))
        (throw+ {:type ::notontologyAnnotations :ontologyAnnotations ontologyAnnotations}))
      (throw+ {:type ::directImports :directImports directImports}))))

(defn ontology
  [ontologyIRI versionIRI directImports ontologyAnnotations axioms]
  (cond
    (and (some? ontologyIRI)(some? versionIRI))(-ontology ontologyIRI versionIRI directImports ontologyAnnotations axioms)
    (some? ontologyIRI)(-ontology ontologyIRI directImports ontologyAnnotations axioms)
    :else (-ontology directImports ontologyAnnotations axioms)))

(defn ontologyIRI [iri]
	"ontologyIRI := IRI"
  (if (:iri iri)
    (assoc iri :type :ontologyIRI :innerType :ontologyIRI)
    (throw+ {:type ::notIRI :IRI iri})))

(defn versionIRI [iri]
	"versionIRI := IRI"
  (if (:iri iri)
    (assoc iri :type :versionIRI :innerType :versionIRI)
    (throw+ {:type ::notIRI :IRI iri})))

(defn directImports [imports]
	"directlyImportsDocuments := { 'Import' '(' IRI ')' }"
  (if (not (empty? imports))
    (if (every? (fn [x] (= (:type x) :import)) imports)
      {:imports imports :type :imports :innerType :imports}
      (throw+ {:type ::notImoprts :imports imports}))
    nil))

(defn directImport [iri]
	"'Import' '(' IRI ')'"
  (if (:iri iri)
  (do (println "Import: " (:short iri))
    (assoc iri :type :import :innerType :import)
    )
    (throw+ {:type ::notIRI :IRIs iri})))

(defn ontologyAnnotations [annotations]
	"ontologyAnnotations := { Annotation }"
  (if (not (empty? annotations))
    (if (every? (fn [x] (= (:type x) :annotation)) annotations)
      {:annotations annotations :type :annotations}
      (throw+ {:type ::notAnnotations :annotations annotations}))
    nil))

(defn axioms [axioms]
	"axioms := { Axiom }"
  (if (not (empty? axioms))
  	(if (every? (fn [x] (= (:type x) :axiom)) axioms)
      {:axioms axioms :type :axioms}
      (throw+ {:type ::notAxioms :Axioms axioms}))
    nil))

(defn ontologyFile
  ([ontology](-ontologyFile ontology))
  ([prefixes ontology](-ontologyFile prefixes ontology)))