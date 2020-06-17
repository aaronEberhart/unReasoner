(ns ontology.axioms
  (:use [slingshot.slingshot :only [throw+]])
  (:require [ontology.annotations :as ann][ontology.components :as co][ontology.expressions :as ex][ontology.SWRL :as swrl]))

(def axiomTypes
  #{:declaration :classAxiom :roleAxiom :dataRoleAxiom :newDataType :hasKey :fact :annotationAxiom :rule :dgAxiom})
(def classAxiomTypes
  #{:classImplication :=Classes :disjClasses :disjOr})
(def roleAxiomTypes
  #{:roleImplication  :=Roles :disjRoles :inverseRoles :roleDomain :roleRange :functionalRole :functionalInverseRole :reflexiveRole :irreflexiveRole :symmetricRole :asymmetricRole :transitiveRole})
(def dataRoleAxiomTypes
  #{:dataRoleImplication :=DataRoles :disjDataRoles :dataRoleDomain :dataRoleRange :functionalDataRole})
(def annotationAxiomTypes
  #{:annotationFact :annotationImplication :annotationDomain :annotationRange})

(defn- -axiom [axiom]
  "Axiom := Declaration | ClassAxiom | ObjectPropertyAxiom | DataPropertyAxiom | DatatypeDefinition | HasKey | Assertion | AnnotationAxiom"
  (if (contains? axiomTypes (:outerType axiom))
    (assoc axiom :type :axiom)
    (throw+ {:type ::notAxiom :axiom axiom})))

(defn- -rule [r]
  "Rule ::= DLSafeRule | DGRule"
  (if (or (= (:type r) :dgRule)(= (:type r) :dlSafeRule))
    (assoc r :outerType :rule)
    (throw+ {:type ::notRule :rule r})))

(defn- -dgRule []
  "DGRule ::= DescriptionGraphRule ‘(’ {Annotation} ‘Body’ ‘(’ {DGAtom} ‘)’ ‘Head’ ‘(’ {DGAtom} ‘)’ ‘)'"
  ;TODO
  )

(defn dgRule
  ([body head](-axiom (-rule (-dgRule body head))))
  ([annotations body head](-axiom (-rule (-dgRule body head)))))

(defn- -dlSafeRule
  "DLSafeRule ::= DLSafeRule ‘(’ {Annotation} ‘Body’ ‘(’ {Atom} ‘)’ ‘Head’ ‘(’ {Atom} ‘)’ ‘)’"
  ([body head]
    (if (= (:type body) :body)
      (if (= (:type head) :head)
        {:body (:atoms body) :head (:atoms head) :type :dlSafeRule :innerType :dlSafeRule :outerType :dlSafeRule}
        (throw+ {:type ::notHead :head head}))
      (throw+ {:type ::notBody :body body})))
  ([annotations body head]
    (if (= (:type body) :body)
      (if (= (:type head) :head)
        (if (= (:type annotations) :axiomAnnotations)
          {:annotations (:annotations annotations) :body (:atoms body) :head (:atoms head) :type :dlSafeRule :innerType :dlSafeRule :outerType :dlSafeRule}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notHead :head head}))
      (throw+ {:type ::notBody :body body}))))

(defn dlSafeRule
  ([body head]
    (-axiom (-rule (-dlSafeRule body head))))
  ([annotations body head]
    (-axiom (-rule (-dlSafeRule (ann/axiomAnnotations annotations) body head)))))

(defn dgAxiom []
  "DGAxiom ::= ‘DescriptionGraph’ ‘(’ {Annotation} DGName DGNodes DGEdges MainClasses ‘)’"
  ;TODO
  )

(defn- -declaration
  "Declaration := 'Declaration' '(' axiomAnnotations Entity ')'"
  ([name]
    (if (= (:type name) :name)
      {:name name :type :declaration :innerType :declaration  :outerType :declaration}
      (throw+ {:type ::notName :name name})))
  ([annotations name]
    (if (= (:type name) :name)
      (if (= (:type annotations) :axiomAnnotations)
        {:name name :annotations (:annotations annotations) :type :declaration :innerType :declaration :outerType :declaration}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notName :name name}))))

(defn declaration
  ([name]
    (-axiom (-declaration name)))
  ([annotations name]
    (-axiom (-declaration annotations name))))

(defn- -classAxiom [classAxiom]
  "ClassAxiom := SubClassOf | EquivalentClasses | DisjointClasses | DisjointUnion"
  (if (contains? classAxiomTypes (:type classAxiom))
    (assoc classAxiom :outerType :classAxiom)
    (throw+ {:type ::notClassAxiom :classAxiom classAxiom})))

(defn- -antecedentClass [class]
  "subClassExpression := ClassExpression"
  (if (= (:type class) :class)
    class;(assoc class :type :antecedentClass)
    (throw+ {:type ::notClass :class class})))

(defn- -consequentClass [class]
  "superClassExpression := ClassExpression"
  (if (= (:type class) :class)
    class;(assoc class :type :consequentClass)
    (throw+ {:type ::notClass :class class})))

(defn- -classImplication
  "SubClassOf := 'SubClassOf' '(' axiomAnnotations subClassExpression superClassExpression ')'"
  ([antecedent consequent]
    (if (and(= (:type antecedent) :class)(= (:type consequent) :class))
      {:antecedentClass antecedent :consequentClass consequent :type :classImplication :innerType :classImplication :outerType :classImplication}
      (throw+ {:type ::notAntecedentConsequentClasses :antecedentClass antecedent :consequentClass consequent})))
  ([annotations antecedent consequent]
    (if (and(= (:type antecedent) :class)(= (:type consequent) :class))
      (if (= (:type annotations) :axiomAnnotations)
        {:antecedentClass antecedent :consequentClass consequent :annotations (:annotations annotations) :type :classImplication :innerType :classImplication :outerType :classImplication}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notAntecedentConsequentClasses :antecedentClass antecedent :consequentClass consequent}))))

(defn classImplication
  ([antecedent consequent];(println antecedent)(prn consequent)
    (-axiom (-classAxiom (-classImplication (ex/class antecedent)(ex/class consequent)))))
  ([annotations antecedent consequent]
    (-axiom (-classAxiom (-classImplication (ann/axiomAnnotations annotations)(ex/class antecedent)(ex/class consequent))))))

(defn- -=Classes
 "EquivalentClasses := 'EquivalentClasses' '(' axiomAnnotations ClassExpression ClassExpression { ClassExpression } ')'"
 ([classes]
    (if (< 1 (count classes))
      (if (every? (fn [x] (= (:type x) :class)) classes)
        {:classes classes :type :=Classes :innerType :=Classes :outerType :=Classes}
        (throw+ {:type ::notClasses :classes classes}))
      (throw+ {:type ::notEnoughClasses :classes classes})))
  ([annotations classes]
    (if (< 1 (count classes))
      (if (every? (fn [x] (= (:type x) :class)) classes)
        (if (= (:type annotations) :axiomAnnotations)
          {:classes classes :annotations (:annotations annotations) :type :=Classes :innerType :=Classes :outerType :=Classes}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notClasses :classes classes}))
      (throw+ {:type ::notEnoughClasses :classes classes}))))

(defn =Classes
  ([classes]
    (-axiom (-classAxiom (-=Classes (into #{} (map ex/class classes))))))
  ([annotations classes]
    (-axiom (-classAxiom (-=Classes (ann/axiomAnnotations annotations) (into #{} (map ex/class classes)))))))

(defn- -disjClasses
 "DisjointClasses := 'DisjointClasses' '(' axiomAnnotations ClassExpression ClassExpression { ClassExpression } ')'"
  ([classes]
    (if (< 1 (count classes))
      (if (every? (fn [x] (= (:type x) :class)) classes)
        {:classes classes :type :disjClasses :innerType :disjClasses :outerType :disjClasses}
        (throw+ {:type ::notClasses :classes classes}))
      (throw+ {:type ::notEnoughClasses :classes classes})))
  ([annotations classes]
    (if (< 1 (count classes))
      (if (every? (fn [x] (= (:type x) :class)) classes)
        (if (= (:type annotations) :axiomAnnotations)
          {:classes classes :annotations (:annotations annotations) :type :disjClasses :innerType :disjClasses :outerType :disjClasses}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notClasses :classes classes}))
      (throw+ {:type ::notEnoughClasses :classes classes}))))

(defn disjClasses
  ([classes]
    (-axiom (-classAxiom (-disjClasses (into #{} (map ex/class classes))))))
  ([annotations classes]
    (-axiom (-classAxiom (-disjClasses (ann/axiomAnnotations annotations) (into #{} (map ex/class classes)))))))

(defn- -disjOr
 "DisjointUnion := 'DisjointUnion' '(' axiomAnnotations Class disjointClassExpressions ')'"
  ([class classes]
    (if (or (= (:type classes) :disjClassesNoAnn)(= (:type classes) :disjClasses))
      (if (= (:type class) :class)
        {:class class :classes (:classes classes) :type :disjOr :innerType :disjOr :outerType :disjOr}
        (throw+ {:type ::notClasses :classes classes}))
      (throw+ {:type ::notEnoughClasses :classes classes})))
  ([annotations class classes]
    (if (= (:type classes) :disjClassesNoAnn)
      (if (= (:type class) :class)
        (if (= (:type annotations) :axiomAnnotations)
          {:classes (:classes classes) :class class :annotations (:annotations annotations) :type :disjOr :innerType :disjOr :outerType :disjOr}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notClasses :classes classes}))
      (throw+ {:type ::notEnoughClasses :classes classes}))))

(defn- -disjClassesNoAnn [classes]
 "disjointClassExpressions := ClassExpression ClassExpression { ClassExpression }"
  (if (< 1 (count classes))
    (if (every? (fn [x] (= (:type x) :class)) classes)
      {:classes classes :type :disjClassesNoAnn :innerType :disjOr}
      (throw+ {:type ::notClasses :classes classes}))
    (throw+ {:type ::notEnoughClasses :classes classes})))

(defn disjOr
  ([class classes]
    (-axiom (-classAxiom (-disjOr (ex/class class) (-disjClassesNoAnn (into #{} (map ex/class classes)))))))
  ([annotations class classes]
    (-axiom (-classAxiom (-disjOr (ann/axiomAnnotations annotations) (ex/class class) (-disjClassesNoAnn (into #{} (map ex/class classes))))))))

(defn- -roleAxiom [roleAxiom]
 "ObjectPropertyAxiom := SubObjectPropertyOf | EquivalentObjectProperties | DisjointObjectProperties | InverseObjectProperties | ObjectPropertyDomain | ObjectPropertyRange | FunctionalObjectProperty | InverseFunctionalObjectProperty | ReflexiveObjectProperty | IrreflexiveObjectProperty | SymmetricObjectProperty | AsymmetricObjectProperty | TransitiveObjectProperty"
  (if (contains? roleAxiomTypes (:type roleAxiom))
    (assoc roleAxiom :outerType :roleAxiom)
    (throw+ {:type ::notRoleAxiom :roleAxiom roleAxiom})))

(defn- -antecedentRole [role]
  "subObjectPropertyExpression := ObjectPropertyExpression | propertyExpressionChain"
  (if (or (= (:type role) :role)(= (:type role) :roleChain))
    role;(assoc role :type :antecedentRole)
    (throw+ {:type ::notRole :role role})))

(defn- -roleChain [roles]
  "propertyExpressionChain := 'ObjectPropertyChain' '(' ObjectPropertyExpression ObjectPropertyExpression { ObjectPropertyExpression } ')'"
  (if (< 1 (count roles))
    (if (every? (fn [x] (= (:type x) :role)) roles)
      {:roles roles :type :roleChain :innerType :roleChain}
      (throw+ {:type ::notRoles :roles roles}))
    (throw+ {:type ::notEnoughRoles :roles roles})))

(defn roleChain
  ([role1 role2 & roles]
    (-roleChain (into [] (map ex/role (flatten [role1 role2 roles])))))
  ([role1 role2]
    (-roleChain [(ex/role role1) (ex/role role2)])))

(defn- -consequentRole [role]
  "superObjectPropertyExpression := ObjectPropertyExpression"
  (if (= (:type role) :role)
    role;(assoc role :type :consequentRole)
    (throw+ {:type ::notRole :role role})))

(defn- -roleImplication
 "SubObjectPropertyOf := 'SubObjectPropertyOf' '(' axiomAnnotations subObjectPropertyExpression superObjectPropertyExpression ')'"
  ([antecedent consequent]
    (if (and (or (= (:type antecedent) :role)(= (:type antecedent) :roleChain))(= (:type consequent) :role))
      {:antecedentRole antecedent :consequentRole consequent :type :roleImplication :innerType :roleImplication :outerType :roleImplication}
      (throw+ {:type ::notAntecedentConsequentRoles :antecedentRole antecedent :consequentRole consequent})))
  ([annotations antecedent consequent]
    (if (and (or (= (:type antecedent) :role)(= (:type antecedent) :roleChain))(= (:type consequent) :role))
      (if (= (:type annotations) :axiomAnnotations)
        {:annotations (:annotations annotations) :antecedentRole antecedent :consequentRole consequent :type :roleImplication :innerType :roleImplication :outerType :roleImplication}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notAntecedentConsequentRoles :antecedentRole antecedent :consequentRole consequent}))))

(defn roleImplication
  ([antecedent consequent]
    (-axiom (-roleAxiom (-roleImplication (ex/role antecedent) (ex/role consequent)))))
  ([annotations antecedent consequent]
    (-axiom (-roleAxiom (-roleImplication (ann/axiomAnnotations annotations)(ex/role antecedent)(ex/role consequent))))))

(defn- -=Roles
 "EquivalentObjectProperties := 'EquivalentObjectProperties' '(' axiomAnnotations ObjectPropertyExpression ObjectPropertyExpression { ObjectPropertyExpression } ')'"
  ([roles]
    (if (< 1 (count roles))
      (if (every? (fn [x] (= (:type x) :role)) roles)
        {:roles roles :type :=Roles :innerType :=Roles :outerType :=Roles}
        (throw+ {:type ::notRoles :roles roles}))
      (throw+ {:type ::notEnoughRoles :roles roles})))
  ([annotations roles]
    (if (< 1 (count roles))
      (if (every? (fn [x] (= (:type x) :role)) roles)
        (if (= (:type annotations) :axiomAnnotations)
          {:roles roles :annotations (:annotations annotations) :type :=Roles :innerType :=Roles :outerType :=Roles}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notRoles :roles roles}))
      (throw+ {:type ::notEnoughRoles :roles roles}))))

(defn =Roles
  ([roles]
    (-axiom (-roleAxiom (-=Roles (into #{} (map ex/role roles))))))
  ([annotations roles]
    (-axiom (-roleAxiom (-=Roles (ann/axiomAnnotations annotations) (into #{} (map ex/role roles)))))))

(defn- -disjRoles
 "DisjointObjectProperties := 'DisjointObjectProperties' '(' axiomAnnotations ObjectPropertyExpression ObjectPropertyExpression { ObjectPropertyExpression } ')'"
  ([roles]
    (if (< 1 (count roles))
      (if (every? (fn [x] (= (:type x) :role)) roles)
        {:roles roles :type :disjRoles :innerType :disjRoles :outerType :disjRoles}
        (throw+ {:type ::notRoles :roles roles}))
      (throw+ {:type ::notEnoughRoles :roles roles})))
  ([annotations roles]
    (if (< 1 (count roles))
      (if (every? (fn [x] (= (:type x) :role)) roles)
        (if (= (:type annotations) :axiomAnnotations)
          {:roles roles :annotations (:annotations annotations) :type :disjRoles :innerType :disjRoles :outerType :disjRoles}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notRoles :roles roles}))
      (throw+ {:type ::notEnoughRoles :roles roles}))))

(defn disjRoles
  ([roles]
    (-axiom (-roleAxiom (-disjRoles (into #{} (map ex/role roles))))))
  ([annotations roles]
    (-axiom (-roleAxiom (-disjRoles (ann/axiomAnnotations annotations) (into #{} (map ex/role roles)))))))

(defn- -roleDomain
 "ObjectPropertyDomain := 'ObjectPropertyDomain' '(' axiomAnnotations ObjectPropertyExpression ClassExpression ')'"
  ([role class]
    (if (and (= (:type role) :role)(= (:type class) :class))
      {:role role :class class :type :roleDomain :innerType :roleDomain :outerType :roleDomain}
      (throw+ {:type ::notClassAndRole :role role :class class})))
  ([annotations role class]
    (if (and (= (:type role) :role)(= (:type class) :class))
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :class class :annotations (:annotations annotations) :type :roleDomain :innerType :roleDomain :outerType :roleDomain}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notClassAndRole :role role :class class}))))

(defn roleDomain
  ([role class]
    (-axiom (-roleAxiom (-roleDomain (ex/role role)(ex/class class)))))
  ([annotations role class]
    (-axiom (-roleAxiom (-roleDomain (ann/axiomAnnotations annotations) (ex/role role)(ex/class class))))))

(defn- -roleRange
 "ObjectPropertyRange := 'ObjectPropertyRange' '(' axiomAnnotations ObjectPropertyExpression ClassExpression ')'"
  ([role class]
    (if (and (= (:type role) :role)(= (:type class) :class))
      {:role role :class class :type :roleRange :innerType :roleRange :outerType :roleRange}
      (throw+ {:type ::notClassAndRole :role role :class class})))
  ([annotations role class]
    (if (and (= (:type role) :role)(= (:type class) :class))
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :class class :annotations (:annotations annotations) :type :roleRange :innerType :roleRange :outerType :roleRange}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notClassAndRole :role role :class class}))))

(defn roleRange
  ([role class]
    (-axiom (-roleAxiom (-roleRange (ex/role role)(ex/class class)))))
  ([annotations role class]
    (-axiom (-roleAxiom (-roleRange (ann/axiomAnnotations annotations) (ex/role role)(ex/class class))))))

(defn- -inverseRoles
 "InverseObjectProperties := 'InverseObjectProperties' '(' axiomAnnotations ObjectPropertyExpression ObjectPropertyExpression ')'"
  ([role otherRole]
    (if (and (= (:type role) :role)(= (:type otherRole) :role))
      {:role role :inverse otherRole :type :inverseRoles :innerType :inverseRoles :outerType :inverseRoles}
      (throw+ {:type ::notRoles :role role :inverse otherRole})))
  ([annotations role otherRole]
    (if (and (= (:type role) :role)(= (:type otherRole) :role))
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :inverse otherRole :annotations (:annotations annotations) :type :inverseRoles :innerType :inverseRoles :outerType :inverseRoles}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRoles :role role :inverse otherRole}))))

(defn inverseRoles
  ([role otherRole]
    (-axiom (-roleAxiom (-inverseRoles (ex/role role)(ex/role otherRole) ))) )
  ([annotations role otherRole]
    (-axiom (-roleAxiom (-inverseRoles (ann/axiomAnnotations annotations) (ex/role role)(ex/role otherRole) ))) ))

(defn- -functionalRole
 "FunctionalObjectProperty := 'FunctionalObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :functionalRole :innerType :functionalRole :outerType :functionalRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :functionalRole :innerType :functionalRole :outerType :functionalRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn functionalRole
  ([role]
    (-axiom (-roleAxiom (-functionalRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-functionalRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -functionalInverseRole
 "InverseFunctionalObjectProperty := 'InverseFunctionalObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :functionalInverseRole :innerType :functionalInverseRole :outerType :functionalInverseRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :functionalInverseRole :innerType :functionalInverseRole :outerType :functionalInverseRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn functionalInverseRole
  ([role]
    (-axiom (-roleAxiom (-functionalInverseRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-functionalInverseRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -reflexiveRole
 "ReflexiveObjectProperty := 'ReflexiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :reflexiveRole :innerType :reflexiveRole :outerType :reflexiveRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :reflexiveRole :innerType :reflexiveRole :outerType :reflexiveRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn reflexiveRole
  ([role]
    (-axiom (-roleAxiom (-reflexiveRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-reflexiveRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -irreflexiveRole
 "IrreflexiveObjectProperty := 'IrreflexiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :irreflexiveRole :innerType :irreflexiveRole :outerType :irreflexiveRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :irreflexiveRole :innerType :irreflexiveRole :outerType :irreflexiveRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn irreflexiveRole
  ([role]
    (-axiom (-roleAxiom (-irreflexiveRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-irreflexiveRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -symmetricRole
 "SymmetricObjectProperty := 'SymmetricObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :symmetricRole :innerType :symmetricRole :outerType :symmetricRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :symmetricRole :innerType :symmetricRole :outerType :symmetricRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn symmetricRole
  ([role]
    (-axiom (-roleAxiom (-symmetricRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-symmetricRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -asymmetricRole
 "AsymmetricObjectProperty := 'AsymmetricObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :asymmetricRole :innerType :asymmetricRole :outerType :asymmetricRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :asymmetricRole :innerType :asymmetricRole :outerType :asymmetricRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn asymmetricRole
  ([role]
    (-axiom (-roleAxiom (-asymmetricRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-asymmetricRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -transitiveRole
 "TransitiveObjectProperty := 'TransitiveObjectProperty' '(' axiomAnnotations ObjectPropertyExpression ')'"
  ([role]
    (if (= (:type role) :role)
      {:role role :type :transitiveRole :innerType :transitiveRole :outerType :transitiveRole}
      (throw+ {:type ::notRole :role role})))
  ([annotations role]
    (if (= (:type role) :role)
      (if (= (:type annotations) :axiomAnnotations)
        {:role role :annotations (:annotations annotations) :type :transitiveRole :innerType :transitiveRole :outerType :transitiveRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notRole :role role}))))

(defn transitiveRole
  ([role]
    (-axiom (-roleAxiom (-transitiveRole (ex/role role)))))
  ([annotations role]
    (-axiom (-roleAxiom (-transitiveRole (ann/axiomAnnotations annotations) (ex/role role))))))

(defn- -dataRoleAxiom [dataRoleAxiom]
 "DataPropertyAxiom := SubDataPropertyOf | EquivalentDataProperties | DisjointDataProperties | DataPropertyDomain | DataPropertyRange | FunctionalDataProperty"
  (if (contains? dataRoleAxiomTypes (:type dataRoleAxiom))
    (assoc dataRoleAxiom :outerType :dataRoleAxiom)
    (throw+ {:type ::notDataRoleAxiom :dataRoleAxiom dataRoleAxiom})))

(defn- -antecedentDataRole [dataRole]
  "subDataPropertyExpression := DataPropertyExpression"
  (if (= (:type dataRole) :dataRole)
    (assoc dataRole :type :antecedentDataRole)
    (throw+ {:type ::notDataRole :dataRole dataRole})))

(defn- -consequentDataRole [dataRole]
  "superDataPropertyExpression := DataPropertyExpression"
  (if (= (:type dataRole) :dataRole)
    (assoc dataRole :type :consequentDataRole)
    (throw+ {:type ::notDataRole :dataRole dataRole})))

(defn- -dataRoleImplication
 "SubDataPropertyOf := 'SubDataPropertyOf' '(' axiomAnnotations subDataPropertyExpression superDataPropertyExpression ')'"
  ([antecedent consequent]
    (if (and(= (:type antecedent) :antecedentDataRole)(= (:type consequent) :consequentDataRole))
      {:antecedentDataRole antecedent :consequentDataRole consequent :type :dataRoleImplication :innerType :dataRoleImplication :outerType :dataRoleImplication}
      (throw+ {:type ::notAntecedentConsequentDataRoles :antecedentDataRole antecedent :consequentDataRole consequent})))
  ([annotations antecedent consequent]
    (if (and(= (:type antecedent) :antecedentDataRole)(= (:type consequent) :consequentDataRole))
      (if (= (:type annotations) :axiomAnnotations)
        {:annotations (:annotations annotations) :antecedentDataRole antecedent :consequentDataRole consequent :type :dataRoleImplication :innerType :dataRoleImplication :outerType :dataRoleImplication}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notAntecedentConsequentDataRoles :antecedentDataRole antecedent :consequentDataRole consequent}))))

(defn dataRoleImplication
  ([antecedent consequent]
    (-axiom (-dataRoleAxiom (-dataRoleImplication (-antecedentDataRole (ex/dataRole antecedent)) (-consequentDataRole (ex/dataRole consequent))))))
  ([annotations antecedent consequent]
    (-axiom (-dataRoleAxiom (-dataRoleImplication (ann/axiomAnnotations annotations)  (-antecedentDataRole (ex/dataRole antecedent)) (-consequentDataRole (ex/dataRole consequent)))))))

(defn- -=DataRoles
 "EquivalentDataProperties := 'EquivalentDataProperties' '(' axiomAnnotations DataPropertyExpression DataPropertyExpression { DataPropertyExpression } ')'"
  ([dataRoles]
    (if (< 1 (count dataRoles))
      (if (every? (fn [x] (= (:type x) :dataRole)) dataRoles)
        {:dataRoles dataRoles :type :=DataRoles :innerType :=DataRoles :outerType :=DataRoles}
        (throw+ {:type ::notRoles :dataRoles dataRoles}))
      (throw+ {:type ::notEnoughDataRoles :dataRoles dataRoles})))
  ([annotations dataRoles]
    (if (< 1 (count dataRoles))
      (if (every? (fn [x] (= (:type x) :dataRole)) dataRoles)
        (if (= (:type annotations) :axiomAnnotations)
          {:dataRoles dataRoles :annotations (:annotations annotations) :type :=DataRoles :innerType :=DataRoles :outerType :=DataRoles}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notRoles :dataRoles dataRoles}))
      (throw+ {:type ::notEnoughDataRoles :dataRoles dataRoles}))))

(defn =DataRoles
  ([dataRoles]
    (-axiom (-dataRoleAxiom (=DataRoles (into #{} (map ex/dataRole dataRoles))))))
  ([annotations dataRoles]
    (-axiom (-dataRoleAxiom (=DataRoles (ann/axiomAnnotations annotations) (into #{} (map ex/dataRole dataRoles)))))))

(defn- -disjDataRoles
 "DisjointDataProperties := 'DisjointDataProperties' '(' axiomAnnotations DataPropertyExpression DataPropertyExpression { DataPropertyExpression } ')'"
  ([dataRoles]
    (if (< 1 (count dataRoles))
      (if (every? (fn [x] (= (:type x) :dataRole)) dataRoles)
        {:dataRoles dataRoles :type :disjDataRoles :innerType :disjDataRoles :outerType :disjDataRoles}
        (throw+ {:type ::notRoles :dataRoles dataRoles}))
      (throw+ {:type ::notEnoughDataRoles :dataRoles dataRoles})))
  ([annotations dataRoles]
    (if (< 1 (count dataRoles))
      (if (every? (fn [x] (= (:type x) :dataRole)) dataRoles)
        (if (= (:type annotations) :axiomAnnotations)
          {:dataRoles dataRoles :annotations (:annotations annotations) :type :disjDataRoles :innerType :disjDataRoles :outerType :disjDataRoles}
          (throw+ {:type ::notAnnotations :annotations annotations}))
        (throw+ {:type ::notRoles :dataRoles dataRoles}))
      (throw+ {:type ::notEnoughDataRoles :dataRoles dataRoles}))))

(defn disjDataRoles
  ([dataRoles]
    (-axiom (-dataRoleAxiom (-disjDataRoles (into #{} (map ex/dataRole dataRoles))))))
  ([annotations dataRoles]
    (-axiom (-dataRoleAxiom (-disjDataRoles (ann/axiomAnnotations annotations) (into #{} (map ex/dataRole dataRoles)))))))

(defn- -dataRoleDomain
 "DataPropertyDomain := 'DataPropertyDomain' '(' axiomAnnotations DataPropertyExpression ClassExpression ')'"
  ([dataRole class]
    (if (and (= (:type dataRole) :dataRole)(= (:type class) :class))
      {:dataRole dataRole :class class :type :dataRoleDomain :innerType :dataRoleDomain :outerType :dataRoleDomain}
      (throw+ {:type ::notDataRoleDataRange :dataRole dataRole :class class})))
  ([annotations dataRole class]
    (if (and (= (:type dataRole) :dataRole)(= (:type class) :class))
      (if (= (:type annotations) :axiomAnnotations)
        {:dataRole dataRole :class  class :annotations (:annotations annotations) :type :dataRoleDomain :innerType :dataRoleDomain :outerType :dataRoleDomain}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notDataRoleDataRange :dataRole dataRole :class class}))))

(defn dataRoleDomain
  ([dataRole class]
    (-axiom (-dataRoleAxiom (-dataRoleDomain (ex/dataRole dataRole) (ex/class class)))))
  ([annotations dataRole class]
    (-axiom (-dataRoleAxiom (-dataRoleDomain (ann/axiomAnnotations annotations) (ex/dataRole dataRole) (ex/class class))))))

(defn- -dataRoleRange
 "DataPropertyRange := 'DataPropertyRange' '(' axiomAnnotations DataPropertyExpression DataRange ')'"
  ([dataRole dataRange]
    (if (and (= (:type dataRole) :dataRole)(= (:type dataRange) :dataRange))
      {:dataRole dataRole :dataRange dataRange :type :dataRoleRange :innerType :dataRoleRange :outerType :dataRoleRange}
      (throw+ {:type ::notDataRoleDataRange :dataRole dataRole :dataRange dataRange})))
  ([annotations dataRole dataRange]
    (if (and (= (:type dataRole) :dataRole)(= (:type dataRange) :dataRange))
      (if (= (:type annotations) :axiomAnnotations)
        {:dataRole dataRole :dataRange  dataRange :annotations (:annotations annotations) :type :dataRoleRange :innerType :dataRoleRange :outerType :dataRoleRange}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notDataRoleDataRange :dataRole dataRole :dataRange dataRange}))))

(defn dataRoleRange
  ([dataRole dataRange](-axiom (-dataRoleAxiom (-dataRoleRange (ex/dataRole dataRole) (co/dataRange dataRange)))))
  ([annotations dataRole dataRange](-axiom (-dataRoleAxiom (-dataRoleRange (ann/axiomAnnotations annotations) (ex/dataRole dataRole) (co/dataRange dataRange))))))

(defn- -functionalDataRole
 "FunctionalDataProperty := 'FunctionalDataProperty' '(' axiomAnnotations DataPropertyExpression ')'"
  ([dataRole]
    (if (= (:type dataRole) :dataRole)
      {:dataRole dataRole :type :functionalDataRole :innerType :functionalDataRole :outerType :functionalDataRole}
      (throw+ {:type ::notDataRole :dataRole dataRole})))
  ([annotations dataRole]
    (if (= (:type dataRole) :dataRole)
      (if (= (:type annotations) :axiomAnnotations)
        {:dataRole dataRole :annotations (:annotations annotations) :type :functionalDataRole :innerType :functionalDataRole :outerType :functionalDataRole}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notDataRole :dataRole dataRole}))))

(defn functionalDataRole
  ([dataRole]
    (-axiom (-dataRoleAxiom (-functionalDataRole (ex/dataRole dataRole)))))
  ([annotations dataRole]
    (-axiom (-dataRoleAxiom (-functionalDataRole (ann/axiomAnnotations annotations) (ex/dataRole dataRole))))))

(defn- -hasKey
  "HasKey := 'HasKey' '(' axiomAnnotations ClassExpression '(' { ObjectPropertyExpression } ')' '(' { DataPropertyExpression } ')' ')'"
  ([class roles dataRoles]
    (if (= (:type class) :class)
      (if (or (< 1 (count roles))(< 1 (count dataRoles)))
        (if (and (every? (fn [x] (= (:type x) :role)) roles)(every? (fn [x] (= (:type x) :dataRole)) dataRoles))
          {:roles roles :dataRoles dataRoles :type :hasKey :innerType :hasKey :outerType :hasKey}
          (throw+ {:type ::notRoles :roles roles :dataRoles dataRoles}))
        (throw+ {:type ::notEnoughKeys :roles roles :dataRoles dataRoles}))
      (throw+ {:type ::notClass :class class})))
  ([annotations class roles dataRoles]
    (if (= (:type class) :class)
      (if (or (< 1 (count roles))(< 1 (count dataRoles)))
        (if (and (every? (fn [x] (= (:type x) :role)) roles)(every? (fn [x] (= (:type x) :dataRole)) dataRoles))
          (if (= (:type annotations) :axiomAnnotations)
            {:roles roles :dataRoles dataRoles :annotations (:annotations annotations) :type :hasKey :innerType :hasKey :outerType :hasKey}
            (throw+ {:type ::notAnnotations :annotations annotations}))
          (throw+ {:type ::notRoles :roles roles :dataRoles dataRoles}))
        (throw+ {:type ::notEnoughKeys :roles roles :dataRoles dataRoles}))
      (throw+ {:type ::notClass :class class}))))

(defn hasKey
  ([class roles dataRoles]
    (-axiom (-hasKey (ex/class class) (into #{} (map ex/role roles)) dataRoles)))
  ([annotations class roles dataRoles]
    (-axiom (-hasKey (ann/axiomAnnotations annotations) (ex/class class) (into #{} (map ex/role roles)) dataRoles))))

(defn- -dataTypeDefinition
  "DatatypeDefinition := 'DatatypeDefinition' '(' axiomAnnotations Datatype DataRange ')'"
  ([dataType dataRange]
    (if (and (= (:type dataType) :dataType)(= (:type dataRange) :dataRange))
      {:dataType dataType :dataRange dataRange :type :newDataType :innerType :newDataType :outerType :newDataType}
      (throw+ {:type ::notDataTypeDef :dataType dataType :dataRange dataRange})))
  ([annotations dataType dataRange]
    (if (and (= (:type dataType) :dataType)(= (:type dataRange) :dataRange))
      (if (= (:type annotations) :axiomAnnotations)
        {:dataType dataType :dataRange dataRange :annotations (:annotations annotations) :type :newDataType :innerType :newDataType :outerType :newDataType}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notDataTypeDef :dataType dataType :dataRange dataRange}))))

(defn dataTypeDefinition
  ([datatype datarange](prn (:innerType (co/dataType datatype))(:innerType (co/dataRange datarange)))
    (-axiom (-dataTypeDefinition (co/dataType datatype) (co/dataRange datarange))))
  ([annotations datatype datarange]
    (-axiom (-dataTypeDefinition (ann/axiomAnnotations annotations) (co/dataType datatype) (co/dataRange datarange)))))

(defn- -annotationAxiom [annotationAxiom]
  "AnnotationAxiom := AnnotationAssertion | SubAnnotationPropertyOf | AnnotationPropertyDomain | AnnotationPropertyRange"
  (if (contains? annotationAxiomTypes (:type annotationAxiom))
    (assoc annotationAxiom :outerType :annotationAxiom)
    (throw+ {:type ::notannotationAxiom :annotationAxiom annotationAxiom})))

(defn- -annotationFact
  "AnnotationAssertion := 'AnnotationAssertion' '(' axiomAnnotations AnnotationProperty AnnotationSubject AnnotationValue ')'"
  ([annotationRole annotationSubject annotationValue]
    (if (and (= (:type annotationSubject) :annotationSubject)(and (= (:type annotationRole) :annotationRole)(= (:type annotationValue) :annotationValue)))
      {:annotationSubject annotationSubject :annotationRole annotationRole :annotationValue annotationValue :type :annotationFact :innerType :annotationFact}
      (throw+ {:type ::notAnnotationSubjectRoleAndValue :annotationSubject annotationSubject :annotationRole annotationRole :annotationValue annotationValue})))
  ([annotations annotationRole annotationSubject annotationValue]
   (if (and (= (:type annotationSubject) :annotationSubject)(and (= (:type annotationRole) :annotationRole)(= (:type annotationValue) :annotationValue)))
     (if (= (:type annotations) :axiomAnnotations)
       {:annotations (:annotations annotations) :annotationSubject annotationSubject :annotationRole annotationRole :annotationValue annotationValue :type :annotationFact :innerType :annotationFact}
       (throw+ {:type ::notAnnotations :annotations annotations}))
     (throw+ {:type ::notAnnotationSubjectRoleAndValue :annotationSubject annotationSubject :annotationRole annotationRole :annotationValue annotationValue :annotations annotations}))))

(defn annotationFact
  ([annotationRole annotationSubject annotationValue]
    (-axiom (-annotationAxiom (-annotationFact (ann/annotationRole annotationRole) (ann/annotationSubject annotationSubject) (ann/annotationValue annotationValue)))))
  ([annotations annotationRole annotationSubject annotationValue]
    (-axiom (-annotationAxiom (-annotationFact (ann/axiomAnnotations annotations) (ann/annotationRole annotationRole) (ann/annotationSubject annotationSubject) (ann/annotationValue annotationValue))))))

(defn- -fromAnnotation [annotationRole]
  "subAnnotationProperty := AnnotationProperty"
  (if (= (:type annotationRole) :annotationRole)
    (assoc annotationRole :type :fromAnnotation)
    (throw+ {:type ::notAnnotationRole :annotationRole annotationRole})))

(defn- -toAnnotation [annotationRole]
  "superAnnotationProperty := AnnotationProperty"
  (if (= (:type annotationRole) :annotationRole)
    (assoc annotationRole :type :toAnnotation)
    (throw+ {:type ::notAnnotationRole :annotationRole annotationRole})))

(defn- -annotationImplication
  "SubAnnotationPropertyOf := 'SubAnnotationPropertyOf' '(' axiomAnnotations subAnnotationProperty superAnnotationProperty ')'"
  ([antecedent consequent]
    (if (and (= (:type antecedent) :fromAnnotation)(= (:type consequent) :toAnnotation))
      {:fromAnnotation antecedent :toAnnotation consequent :type :annotationImplication :innerType :annotationImplication}
      (throw+ {:type ::notAnnotationRoles :antecedent antecedent :consequent consequent})))
  ([annotations antecedent consequent]
    (if (and (= (:type antecedent) :fromAnnotation)(= (:type consequent) :toAnnotation))
      (if (= (:type annotations) :axiomAnnotations)
        {:fromAnnotation antecedent :toAnnotation consequent :type :annotationImplication :innerType :annotationImplication :annotations (:annotations annotations)}
        (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notAnnotationRoles :antecedent antecedent :consequent consequent}))))

(defn annotationImplication
  ([antecedent consequent]
    (-axiom (-annotationAxiom (-annotationImplication (-fromAnnotation (ann/annotationRole antecedent)) (-toAnnotation (ann/annotationRole consequent))))))
  ([annotations antecedent consequent]
    (-axiom (-annotationAxiom (-annotationImplication (ann/axiomAnnotations annotations) (-fromAnnotation antecedent) (-toAnnotation consequent))))))

(defn- -annotationDomain
  "AnnotationPropertyDomain := 'AnnotationPropertyDomain' '(' axiomAnnotations AnnotationProperty IRI ')'"
  ([annotationRole IRI]
    (if (and (= (:type annotationRole) :annotationRole)(:iri IRI))
      {:annotationRole annotationRole :iri IRI :type :annotationDomain :innerType :annotationDomain}
      (throw+ {:type ::notAnnotationDomain :annotationRole annotationRole :iri IRI})))
  ([annotations annotationRole IRI]
    (if (and (= (:type annotationRole) :annotationRole)(:iri IRI))
        (if (= (:type annotations) :axiomAnnotations)
          {:annotationRole annotationRole :iri IRI :type :annotationDomain :innerType :annotationDomain :annotations (:annotations annotations)}
          (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notAnnotationDomain :annotationRole annotationRole :iri IRI}))))

(defn annotationDomain
  ([annotationRole IRI]
    (-axiom (-annotationAxiom (-annotationDomain (ann/annotationRole annotationRole) IRI))))
  ([annotations annotationRole IRI]
    (-axiom (-annotationAxiom (-annotationDomain (ann/axiomAnnotations annotations) (ann/annotationRole annotationRole) (co/IRI IRI))))))

(defn- -annotationRange
  "AnnotationPropertyRange := 'AnnotationPropertyRange' '(' axiomAnnotations AnnotationProperty IRI ')'"
  ([annotationRole IRI]
    (if (and (= (:type annotationRole) :annotationRole)(:iri IRI))
      {:annotationRole annotationRole :iri IRI :type :annotationRange :innerType :annotationRange}
      (throw+ {:type ::notAnnotationRange :annotationRole annotationRole :iri IRI})))
  ([annotations annotationRole IRI]
    (if (and (= (:type annotationRole) :annotationRole)(:iri IRI))
        (if (= (:type annotations) :axiomAnnotations)
          {:annotationRole annotationRole :iri IRI :type :annotationRange :innerType :annotationRange :annotations (:annotations annotations)}
          (throw+ {:type ::notAnnotations :annotations annotations}))
      (throw+ {:type ::notAnnotationRange :annotationRole annotationRole :iri IRI}))))

(defn annotationRange
  ([annotationRole IRI]
    (-axiom (-annotationAxiom (-annotationRange (ann/annotationRole annotationRole) IRI))))
  ([annotations annotationRole IRI]
    (-axiom (-annotationAxiom (-annotationRange (ann/axiomAnnotations annotations) (ann/annotationRole annotationRole) IRI)))))
