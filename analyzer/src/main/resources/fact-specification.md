## LogicFL's Facts Specifications

### Semantic Facts

Semantic facts represent the meaning of code fragments observed in source code.

#### argument(?arg_expr, ?index, ?m_expr).
* Corresponding Java Class: ``logicfl.locgic.codefacts.Argument``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    * arg_expr: the argument expression.
    * index: the argument is i-th argument of the method invocation.
    * m_expr: the method invocation which the argument is used.


#### assign(?lhs, ?rhs, ?line:Line).
* Corresponding Java Class: ``logicfl.locgic.codefacts.Assign``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  lhs: the left hand side of the assignment.
    *  rhs: the right hand side of the assignment.
    *  line: the line which the assignment appears.

#### cond_expr(?boolExpr, ?then_expr, ?else_expr, ?line:Line).
* Corresponding Java Class: ``logicfl.locgic.codefacts.CondExpr``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  bool_expr: the boolean expression (condition) of this conditional expression.
    *  then_expr: the expression when bool_expr is true.
    *  else_expr: the expression when bool_expr is flase.
    *  line: the line of the conditional expression.

#### method_invoc(?invoc_expr, ?method_id, ?line:Line).
* Corresponding Java Class: ``logicfl.locgic.codefacts.MethodInvoc``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    * invoc_expr: the expression of the method invocation.
    * method_id: the method id of the callee.
    * line: the line which the method is invoked.

#### param(?param_id, ?i:int, ?method_id).
* Corresponding Java Class: ``logicfl.locgic.codefacts.Param``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  param_id: the id of the parameter.
    *  i: the index of the parameter.
    *  method_id: the id of the method.

#### ref(?refExpr, ?expr, ?line:Line).

``ref(refExpr, expr, line)`` indicates that ``refExpr`` is referenced in ``expr`` at ``line``.

* Corresponding Java Class: ``logicfl.locgic.codefacts.Ref``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  refExpr: the referenced expression.
    *  expr: the expression which the ``refExpr`` was referenced.
    *  line: line which the ``refExpr`` is referenced.

#### return(?ret_val, ?method_id, ?line:Line).
* Corresponding Java Class: ``logicfl.locgic.codefacts.Return``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  ret_val: the expression representing the return value.
    *  method_id: the method id of the return statement.
    *  line: the line of the return statement.

#### test_failure(?failure_id, ?test_class, ?test_method).

``test_failure/3`` has a list of ``trace/6`` predicates.

* Corresponding Java Class: ``logicfl.locgic.codefacts.TestFailure``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  failure_id: the id of test failure.
    *  test_class: the name of test class.
    *  test_method: the name of test method.

#### throw(?method, ?exception, ?line:Line).

``throw(method, exception)`` can represent ``exception`` is thrown by ``method``.
``throw/2`` can be used for ``throw`` in a method declarations.

* Corresponding Java Class: ``logicfl.locgic.codefacts.Throw``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  method: either the method name or the expression of the method invocation.
    *  exception: the name of the callee.
    *  line: the line which the method is invoked. (optional)

#### trace(?trace_id, ?parent_id, ?method_id, ?line:Line, ?failure_id, ?is_target).

``parent_id`` of the ``trace/6`` at the stack top is ``failure_id``.

* Corresponding Java Class: ``logicfl.locgic.codefacts.Trace``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  trace_id: the id of this stack trace.
    *  parent_id: the id of the parent stack trace, which calls a method in this stack trace.
    *  method_id: the method id which this stack trace indicates.
    *  line: the line of this stack trace.
    *  failure_id: the id of test failure which this stack trace belongs.
    *  is_target: ``target`` indicates that the method can be found in code base, otherwise ``non_target``.

#### val(?expr, ?val, ?line:Line).

A ``val/3`` predicate is used to represent an observed value of an expression at a certain location.
It is generated by ``DynamicAnalyzer``.

* Corresponding Java Class: ``logicfl.locgic.codefacts.Val``
* Paraent Class: ``logicfl.locgic.Predicate``
* Parameters
    *  expr: an expression
    *  val: the value of the expression
    *  line: the line where the expression has the value</lib>

### Code Facts

Code facts are mainly used to connect code entities in Prolog to actual Java code entities in source code.

#### code(?code_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range).
* Corresponding Java Class: ``logicfl.locgic.codefacts.CodeEntity``
* Parameters
    *  code_id: the id of this code entity.
    *  node_type: AST node type of this code entity.
    *  parent_id: the id of the parent node. (optional)
    *  loc_in_parent: the code entity's locaton in its parent. (optional)
    *  range: code range of this code entity.

#### method(?method_id, ?range:Range).
* Corresponding Java Class: ``logicfl.logic.codefacts.MethodDecl``
* Paraent Class: ``logicfl.locgic.codefacts.CodeEntity``
* Parameters
    *  method_id: the declared method id.
    *  range: code range of the method.

#### block(?block_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range).
* Corresponding Java Class: ``logicfl.logic.codefacts.CodeBlock``
* Paraent Class: ``logicfl.locgic.codefacts.CodeEntity``
* Parameters
    *  block_id: the id of this block.
    *  node_type: AST node type of this block.
    *  parent_id: the id of the parent node.
    *  loc_in_parent: the block's locaton in its parent.
    *  range: code range of this block.

#### stmt(?stmt_id, ?node_type, ?parent_id:CodeBlock, ?loc_in_parent, ?range:Range).
* Corresponding Java Class: ``logicfl.logic.codefacts.Stmt``
* Paraent Class: ``logicfl.locgic.codefacts.CodeEntity``
* Parameters
    *  stmt_id: the id of this statement.
    *  node_type: AST node type of this statement.
    *  parent_id: the id of the parent node.
    *  loc_in_parent: the statement's locaton in its parent ('statments', index).
    *  range: code range of this statement.

#### expr(?expr_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range, ?code).
* Corresponding Java Class: ``logicfl.logic.codefacts.Expr``
* Paraent Class: ``logicfl.locgic.codefacts.CodeEntity``
* Special Case: ``ExprNone`` to represent an empty expression.
* Parameters
    *  expr_id: the id of this expression.
    *  node_type: AST node type of this expression.
    *  parent_id: the id of the parent node. (optional)
    *  loc_in_parent: the expression's locaton in its parent. (optional)
    *  range: code range of this expression.
    *  code: actual code of the expression.

#### name(?name_ref_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range, ?name).

Note that ``name/6`` usese ``name_ref_id`` as its id. 
If the same name appears multiple times, there will be one ``name_ref/4`` predicate with multiple ``name/6`` predicates.

* Corresponding Java Class: ``logicfl.logic.codefacts.CodeName``
* Paraent Class: ``logicfl.locgic.codefacts.Expr``
* Parameters
    *  name_ref_id: the id of this name.
    *  node_type: AST node type of this name.
    *  parent_id: the id of the parent node.
    *  loc_in_parent: the name's locaton in its parent.
    *  range: code range of this name.
    *  name: actual string name.

#### literal(?literal_id, ?node_type, ?parent_id, ?loc_in_parent, ?range:Range, ?value).
* Corresponding Java Class: ``logicfl.logic.codefacts.Literal``
* Paraent Class: ``logicfl.locgic.codefacts.Expr``
* Parameters
    *  literal: the id of this literal.
    *  node_type: AST node type of this literal.
    *  parent_id: the id of the parent node.
    *  loc_in_parent: the literal's locaton in its parent.
    *  range: a code range of this literal.
    *  value: an actual value of this literal.

#### name_ref(?name_ref_id, ?name_type, ?name, ?binding_key).

``name_ref/4`` predicates to contain simple binding information of names.

* Corresponding Java Class: ``logicfl.logic.codefacts.NameRef``
* Parameters
    *  name_ref_id: the id of this name.
    *  name_type: the type of this name - varible, field, parameter, method, etc.
    *  name: actual string name.
    *  binding_key: a binding key of this name generated by Eclipse JDT.`


#### range(?class_id, ?start_pos:int, ?length:int, ?start_line:int, ?end_line:int).
* Corresponding Java Class: ``logicfl.logic.codefacts.Range``
* Parameters
    *  class_id: the class id of this range.
    *  start_pos: the start index of a code range in source code.
    *  length: the length of this code range.
    *  start_line: the start line of this code range.
    *  end_line: the end line of this code range.


#### line(?class_id, ?line_num:int)``
* Corresponding Java Class: ``logicfl.logic.codefacts.Line``
* Parameters
    *  {@code class_id}: the class id of this code line.
    *  {@code line_num}: the line number of this code line.
