## LogicFL's Rules Specifications

LogicFL leverages several sets of rules defined to represent different knowledge related to identify causes of Null Pointer Exception (NPE). These rules are defined based on facts collected from source code and test execution. (i.e., work with facts defined in fact specification.)

Each Knowledge Base (KB) should be constructed for each NPE bug. Mixing facts from different bugs may lead to unexpected results. 

### Starting Point

#### find_npe_cause(Expr, Line, Cause, Loc).

*Parameters*
* ``Expr``: an expression which is null and causes an NPE.
* ``Line``: a code line where an NPE is thrown.
* ``Cause``: an identified cause (code fragment) which makes ``Expr`` null to throw an NPE.
* ``Loc``: a code line where the ``Cause`` exists.

*Rule definition*
```
find_npe_cause(Expr, Line, Cause, Loc) :- 
    findall((Expr, Line, Cause, Loc), (npe(Expr, Line), cause_of(npe(Expr, Line), Cause, Loc)), TotalCauses),
    remove_duplicates(TotalCauses, UniqueCauses),
    rank_causes(UniqueCauses, RankedCauses),
    member((Expr, Line, Cause, Loc), RankedCauses).
```

This predicate is used for the main query. It will identify all possible ``Expr`` and ``Line`` pairs identified by ``npe(Expr, Line)``, which may cause an NPE in the KB. If you want to identify causes for a specific NPE, caused by a certain expression or thrown at a certain line, you can set ``Expr`` and ``Line`` repsectively.

It removes duplicates for a tuple (``Expr``, ``Line``, ``Cause``, ``Loc``) using ``remove_duplicates/2``, and rank identified candidates with ``rank_causes/2``, then provides each candidate one-by-one with ``member/2``.

### Rules to Identify Null Expressions

#### npe(Expr, Line).

*Parameters*
* ``Expr``: an expression which is null and causes the NPE.
* ``Line``: a code line where an NPE is thrown.

*Rule definition*
```
npe(Expr, Line) :- 
    candidate_line(Line), 
    find_null_expr(Expr, Line).
```

This predicate is used to find a candidate expression ``Expr`` which is responsible for an NPE thrown at line ``Line``.
It first finds candidate lines from stact traces using ``candidate_line/1``.
Then it searches for null expressions using ``find_null_expr/2``, which are null at the identified candidate lines.

#### candidate_line(Line).

*Parameters*
* ``Line``: a candidate code line appeared in stack traces.

*Rule definition*
```
candidate_line(Line) :- 
    test_failure(Failure, _, _),
    first_target_trace(_, Line, Failure).
```

``candidate_line/1`` identifies candidate lines which may appeared in stack traces when an NPE is thrown.
If there are multiple test failures, it will provide a candidate line for each test failure.
``test_failure/3`` is a semantic fact representing test execution information.

#### first_target_trace(-Trace, -Line, +Failure).

*Parameters*
* ``Trace``: the id of the first target trace. (return)
* ``Line``: a candidate code line appeared in stack traces. (return)
* ``Failure``: a failure id representing an NPE.

*Rule definition*
```
first_target_trace(Trace, Line, Failure) :- 
    target_trace(Trace, Failure, Line, Failure), 
    \+throw_at_line(Line).
first_target_trace(Trace, Line, Failure) :- 
    target_trace(Trace, Next, Line, Failure),
    Next \== Failure,
    (is_non_target(Next) ; 
    	trace_line(Next, NextLine, Failure),
    	throw_at_line(NextLine)), !.
throw_at_line(line(ClassId, LineNum)) :- 
    stmt(_, throw_statement, _, _, range(ClassId, _, _, LineNum, _)).        
```

``first_target_trace/3`` returns the id of the first target trace ``Trace`` and its code line ``Line``, for a given test failure ``Failure``.
A target trace indicates that the code line in the trace can be accessed by the current code base.
This information is given as a fact of ``trace/6``.
``target_trace/4`` is a helper predicate to retrieve only target traces.
Note that if an exception is thrown directly by a ``throw_statement``, this code line is ignored and the trace representing the caller is selected as the first trace (``throw_at_line/1``).


#### find_null_expr(Expr, Line).

*Parameters*
* ``Expr``: an expression which is null and causes the NPE.
* ``Line``: a code line where an NPE is thrown.

*Rule definition*
```
find_null_expr(Expr, Line) :- 
    findall((Expr, Line), null_arg_passed(Expr, Line) ; null_ref(Expr, Line), Candidates),
    remove_duplicates(Candidates, NullExprs),
    member((Expr, Line), NullExprs).
```

``find_null_expr/2`` finds all null expressions which are null at the identified candidate lines.
It first lists up all null expressions identified by ``null_arg_passed/2`` and ``nel_ref``.
Then it removes duplicates and provides candidate expressions one-by-one.


#### null_arg_passed(Expr, Line).

*Parameters*
* ``Expr``: an expression which is null and causes the NPE.
* ``Line``: a code line where an NPE is thrown.

*Rule definition*
```
null_arg_passed(Expr, Line) :-
    trace_called_method(_, M, Line),
    find_null_arg(Expr, Line, _, M).
trace_called_method(Trace, Method, Line) :-
    trace(Trace, Callee, _, Line, Failure, _),
    (trace(Callee, _, Method, _, Failure, target) ;
        (trace(Callee, _, MethodName, _, Failure, non_target),
        called_method(Method, MethodName, Line))).
find_null_arg(Expr, Line, MI, M) :-
    findall((Expr, Line, MI, M), null_arg(Expr, Line, MI, M), NullArgs),
    remove_duplicates(NullArgs, Unique),
    member((Expr, Line, MI, M), Unique).
null_arg(Expr, Line, MI, M) :-
    val(Expr, null, Line),
    argument(Expr, _, MI),
    method_invoc(MI, M, Line).    
```

``null_arg_passed/2`` identifies a null argument ``Expr`` passed at ``Line``.
It first finds a method ``M`` called at ``Line`` using ``trace_called_method/3``.
Note that ``M`` should be appeared in stack traces, to ensure that this is dynamically called and relevant to an NPE.
Consider a code line ``method1(method2(arg1, arg2), method3(arg3))``. 
If ``method2`` is the one throws an NPE, then it will be identified as ``M`` by ``trace_called_method/3``.
Then the next step is find a null argument of ``M``. 
In the example, ``arg1`` and ``arg2`` can be candidates, and ``find_null_arg/4`` finds all arguments which are null, by applying ``null_arg/4``.


#### null_ref(Expr, Line).

*Parameters*
* ``Expr``: an expression which is null and causes the NPE.
* ``Line``: a code line where an NPE is thrown.

*Rule definition*
```
null_ref(Expr, Line) :- 
    ref(Expr, _, Line), check_val(Expr, null, Line).  
```

``null_ref/2`` identifies a null expression ``Expr`` which is referenced at ``Line``.
Referenced expressions can be found with facts ``ref/3``.
Then for each referenced expression, ``check_val/3`` verifies whether ``Expr`` is null at ``Line``.

#### null_ref(Expr, Line).

*Parameters*
* ``Expr``: an expression which is null and causes the NPE.
* ``Line``: a code line where an NPE is thrown.

*Rule definition*
```
null_ref(Expr, Line) :- 
    ref(Expr, _, Line), check_val(Expr, null, Line).  
```

``null_ref/2`` identifies a null expression ``Expr`` which is referenced at ``Line``.
Referenced expressions can be found with facts ``ref/3``.
Then for each referenced expression, ``check_val/3`` verifies whether ``Expr`` is null at ``Line``.

### Rules to Identify Candidates of NPE Causes

#### cause_of(npe(Expr, Line), Cause, Loc).

*Parameters*
* ``Expr``: an expression which is null and causes an NPE.
* ``Line``: a code line where an NPE is thrown.
* ``Cause``: an identified cause (code fragment) which makes ``Expr`` null to throw an NPE.
* ``Loc``: a code line where the ``Cause`` exists.

*Rule definition*
```
cause_of(npe(Expr, Line), Cause, Loc) :- 
    null_arg_passed(Expr, Line),
    Cause = Expr, Loc = Line.
cause_of(npe(Expr, Line), Cause, Loc) :- 
    null_arg_passed(Expr, Line),
    originated_from(val(Expr, null, Line), (Expr1, Line1)),
    Cause = Expr1, Loc = Line1.
cause_of(npe(Expr, Line), Cause, Loc) :-
    null_ref(Expr, Line),
    Cause = Expr, Loc = Line.
cause_of(npe(Expr, Line), Cause, Loc) :-
    null_ref(Expr, Line),
    originated_from(val(Expr, null, Line), (Expr1, Line1)),    
    Cause = Expr1, Loc = Line1.
cause_of(npe(Expr, Line), Cause, Loc) :-
    (null_arg_passed(Expr, Line) ; null_ref(Expr, Line)),
    can_be_transferred(val(Expr, null, Line), (Cause, Loc)). 
```

``cause_of/3`` identifies NPE causes (``Cause``) and their locations (``Loc``) for each null expression ``Expr`` at ``Line``.
There are five rules for ``cause_of/3`` which produces candidates in a specific order.
Firstly, null arguments (``null_arg_passed/2``) are considered, and origins of the null argumnets are considered after that.
Next, null references (``null_ref/2``) and their origins are considered.
Finally, any expressions which may transfer null value to null arguments and null references are considered.

### Rules to Rank Candidates

#### rank_causes(+Total, -Ranked).

*Parameters*
* ``Total``: a list of candidates need to be ranked.
* ``Ranked``: a list of ranked candidates.

*Rule definition*
```
rank_causes(Total, Ranked) :- 
    exclude(filter_cond, Total, Filtered),
    include(prefer_cond, Filtered, Preferred),
    subtract(Filtered, Preferred, Remaining),
    append(Preferred, Remaining, Ranked).  

prefer_cond((_, _, Cause, Loc)) :- 
    find_method(Method, Loc), 
    (is_null_return(Method, Cause) ;
    	val_assigned_in_method(Cause, Loc, Method) ; 
    	only_target_method(Method)), !.
filter_cond((_, _, _, Loc)) :- from_test(Loc), !.
filter_cond((_, _, _, Loc)) :- find_method(Method, Loc), arg_passing_method(Method), !.
```

``rank_causes/2`` applies ``prefer_cond/1`` and ``filter_cond/`` to rank given candidates.
It first excludes all candidates which fit to ``filter_cond/1``.
Then it ranks up candidates which satisfy ``prefer_cond/1``.
The original order of candidates in ``Total`` is preserved during this process.
Note that ``find_method/2`` and ``from_test/1`` predicates are considered as rules to describe common behaviour (or knowledge) about programs, not specially designed for NPEs.


#### is_null_return(Method, Cause).

*Parameters*
* ``Method``: the id of a method which ``Cause`` belongs.
* ``Cause``: a code fragment identified as a candidate.

*Rule definition*
```
is_null_return(Method, Cause) :-
    return(Cause, Method, Line),
    return(Expr, Method, Line1),
    Cause \== Expr, Line \== Line1,
    literal(Cause, null),
    \+ literal(Expr, null).
```

``is_null_return(Method, Cause)`` checks that ``Cause`` is a null literal returned in ``Method``, and there is another return statement returns other than a null literal.
Currently, facts ``return/3`` are only collected if they are excuted. 
Hence ``is_null_return/2`` indicates that ``Method`` is called multiple times, and it returns something other than null.

#### val_assigned_in_method(Expr, Line, Method).

*Parameters*
* ``Expr``: a list of candidates need to be ranked.
* ``Line``: a code line which ``Expr`` exists.
* ``Method``: the id of a method which ``Expr`` belongs.

*Rule definition*
```
val_assigned_in_method(Expr, Line, Method) :- 
    assign(Expr, _, Line1), find_method(Method, Line1), 
    Line = line(_, LineNum), Line1 = line(_, LineNum1),
    LineNum >= LineNum1.
```

For given ``Expr`` and ``Line``, ``val_assigned_in_method/3`` checks whether there is an assignment at ``Line1`` to ``Expr`` in the same method which ``Line`` belongs.
This indicates that it is possible that ``Expr``'s value is changed in ``Method``, at ``Line1``.
Note that ``Line1`` can be identical to ``Line``, since ``Expr`` can be identified as a candidate due to the null value assigned at ``Line1``.

#### only_target_method(Method).

*Parameters*
* ``Method``: the id of a method which a candidate belongs.

*Rule definition*
```
only_target_method(Method) :- 
    trace(Trace, Callee, Method, _, Failure, target),    
    trace(Callee, _, _, _, Failure, non_target),
    trace(_, Trace, _, Line, Failure, target),
    from_test(Line).
```

``only_target_method/1`` checks whether ``Method`` is the only target method appeared in stack straces.
Note that other target methods from tests will be ignored by the third (``trace/6``) and fourth (``from_test/1``) sub-goals.

#### arg_passing_method(Method).

*Parameters*
* ``Method``: the id of a method which a candidate belongs.

*Rule definition*
```
arg_passing_method(Method) :-
    find_method_body(Method, Body),
    %if a return stmt. is the first, it should be the only one.
    stmt(_, return_statement, Body, (statements, 0), range(ClassId, _, _, Line, _)),
    %the return stmt has a method invoc, which directly passes a param. as an arg.
    return(Expr, _, line(ClassId, Line)),
    method_invoc(Expr, Callee, _),
    argument(Arg, _, Expr),
    param(Arg, _, Method),
    %Callee should be another method in the code base.
    method(Callee, _), !.
```

``arg_passing_method/1`` verifies whether ``Method`` is simply passing its arguments to another method.
First, it checks ``Method``'s body only contains a return statement.
Then if the return statement's expression is a method invocation which has arguments directly from parameters of ``Method``, this method is considered as an argument passing method.

### Rules to Describe Common Behaviour

#### originated_from(val(Expr, Val, Line), (Origin, Loc)).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.
* ``Origin``: an origin expression of the value ``Val`` transferred to ``Expr`` at ``Line``.
* ``Loc``: the code line where ``Origin`` exists.

*Rule definition*
```
originated_from(val(Expr, Val, Line), (Origin, Loc)) :-    
    (not_from_others(Expr, Val, Line), ! ; val_first_introduced(Expr, Val, Line)),
    Origin = Expr, Loc = Line.
originated_from(val(Expr, Val, Line), (Origin, Loc)) :-    
    can_be_transferred(val(Expr, Val, Line), (Expr1, Line1)),
    (not_from_others(Expr1, null, Line1), ! ; val_first_introduced(Expr1, Val, Line1)),
    Origin = Expr1, Loc = Line1.
```

``originated_from/2`` traces back to an origin of the value ``val(Expr, Val, Line)``.
More specifically, it find a pair ``(Origin, Loc)`` which can transfer ``Val`` to ``Expr`` at ``Line``, while there is no other expressions passing the value to ``Origin`` at ``Loc``.
To identify such cases, it applies ``not_from_others/3`` and ``val_first_introduced/3``.

#### can_be_transferred(val(Expr, Val, Line), (Expr1, Line1)).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.
* ``Expr1``: another expression which transfers a value to ``Expr`` at ``Line``.
* ``Line1``: the code line where ``Expr1`` exists.

*Rule definition*
```
can_be_transferred(val(Expr, Val, Line), (Expr1, Line1)) :-
    findall((Expr1, Line1), can_be_transferred_internal(val(Expr, Val, Line), (Expr1, Line1)), Pairs),
    remove_duplicates(Pairs, UniquePairs),
    member((Expr1, Line1), UniquePairs).
can_be_transferred_internal(val(Expr, Val, Line), (Expr1, Line1)) :-
	single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)) ;
    multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), [(Expr, Line)]).
```

``can_be_transferred/2`` lists up all possible expressions and their locations ``(Expr1, Line1)`` which may transfer the value ``Val`` to ``Expr`` at ``Line``, by applying ``can_be_transferred_internal/2``.
``can_be_transferred_internal/2`` is used to identify individual case which transfers the value to the expression, using ``single_step_transfer/2`` and ``multi_step_transfer/3``.

#### single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.
* ``Expr1``: another expression which transfers a value to ``Expr`` at ``Line``.
* ``Line1``: the code line where ``Expr1`` exists.

*Rule definition*
```
single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)) :-
    copied_from_expr(val(Expr, Val, Line), (Expr1, Line1)).
single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)) :-
    is_var(Expr),
    assigned_to_var(val(Expr, Val, Line), (Expr1, Line1)).
single_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered) :-
    single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)),
    \+ memberchk((Expr1, Line1), Considered).
```

``single_step_transfer/2`` describes cases which ``val(Expr, Val, Line)`` is directly transferred from ``(Expr1, Line1)``.
``single_step_transfer/3`` is used to prevent a cycle of value transfers.
For instance, consider the following case.

```
public Object method1() {
    ...
    return var1.method2();
}
public Object method2() {
    return method1();    
}

```
In ``method1()``, ``var1.method2()`` is returned. The expression's value is the return value of ``method2()``, which is again ``method1()``'s return value, ``var1.method2()``. 
During actuall execution, different method calls can be distinguished in the call stack, but in the code, the expressions are the same.
Hence ``Considered`` contains expressions already appeared on the path of value transfer, and used to prevent a cycle.


#### multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.
* ``Expr1``: another expression which transfers a value to ``Expr`` at ``Line``.
* ``Line1``: the code line where ``Expr1`` exists.

*Rule definition*
```
multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered) :-
    single_step_transfer(val(Expr, Val, Line), (Expr2, Line2), Considered),
    single_step_transfer(val(Expr2, Val, Line2), (Expr1, Line1), [(Expr2, Line2)|Considered]).
multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered) :-
    single_step_transfer(val(Expr, Val, Line), (Expr2, Line2), Considered),
    multi_step_transfer(val(Expr2, Val, Line2), (Expr1, Line1), [(Expr2, Line2)|Considered]).
```

``multi_step_transfer/3`` is used to identify expressions which may transfer the value ``Val`` with multiple steps.
It is either two ``single_step_transfer/2`` or ``single_step_transfer/2`` + ``multi_step_transfer/2``.
Note that ``Considered`` is also used to prevent a cycle.

#### copied_from_expr(val(Expr, Val, Line), (Expr1, Line1)).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.
* ``Expr1``: another expression which transfers a value to ``Expr`` at ``Line``.
* ``Line1``: the code line where ``Expr1`` exists.

*Rule definition*
```
copied_from_expr(val(Expr, Val, Line), (Expr1, Line1)) :-
    find_candidates((Expr1, Line1), copied_from((Expr, Line), (Expr1, Line1))),
    Expr1 \== Expr,
    check_val(Expr1, Val, Line1).
find_candidates((Expr, Line), Pred) :-
    findall((Expr, Line), Pred, Exprs),
    remove_duplicates(Exprs, Unique),
    member((Expr, Line), Unique).
```

``copied_from_expr/2`` indicates a case that the value ``Val`` of ``Expr`` at ``Line`` is directly copied from another expression ``Expr1`` at ``Line1``.
To identify such expressions, it first finds all candidates which may pass the value to ``Expr`` using ``copied_from/2``, then checks each candidate has the value ``Val`` using ``check_val/3``.
``find_candidates/2`` is used to list up ``(Expr, Line)`` pairs satisfying a given predicate ``Pred``.

#### assigned_to_var(val(Name, Val, Line), (Expr1, Line1)).

*Parameters*
* ``Name``: a name of a variable.
* ``Val``: the value of ``Name``.
* ``Line``: the code line where ``Name`` exists.
* ``Expr1``: an origin expression of the value ``Val`` transferred to ``Name`` at ``Line``.
* ``Line1``: the code line where ``Origin`` exists.

*Rule definition*
```
assigned_to_var(val(Name, Val, Line), (Expr1, Line1)) :-
    check_val(Name, Val, Line),
    assign(Name, Expr1, Line1),
    check_val(Expr1, Val, Line1).
```

``assigned_to_var/2`` finds an expression ``Expr1`` at ``Line1`` which is assigned to a variable ``Name``.
Note that ``Name`` is checked with ``is_var/1`` before ``assigned_to_var/2`` is applied, in ``single_step_transfer/2``.

#### not_from_others(Expr, Val, Line).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.

*Rule definition*
```
not_from_others(Expr, Val, Line) :-
    check_val(Expr, Val, Line),
    \+ ((single_step_transfer(val(Expr, Val, Line), (_, Line1)), \+ from_test(Line1))).
```

``not_from_others/3`` decides whether ``Expr``'s value ``Val`` at ``Line`` is not copied from elsewhere.
First it checks ``Expr`` indeed has the value, then verifies whether it is transferred from other expression.
Note that if the value comes from a test, it is considered not copied from others (``from_test/1``).

#### val_first_introduced(Name, Val, Line).

*Parameters*
* ``Name``: a name of a variable.
* ``Val``: the value of ``Name``.
* ``Line``: the code line where ``Name`` exists.

*Rule definition*
```
val_first_introduced(Name, Val, Line) :-
    is_var(Name),
    (\+ assigned_to_var(val(Name, Val, Line), (_, _))), 
    \+single_step_transfer(val(Name, Val, Line), (_, _)), !.
val_first_introduced(Expr, Val, Line) :-
    check_val(Expr, Val, Line),
    forall(
        single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)),
        \+ has_val(Expr1, Val, Line1)
    ), !.
```

``val_first_introduced/3`` checks whether ``Expr`` at ``Line`` has ``Val`` for the first time.
The first rule checks whether a ``Name``'s value ``Val`` is not assigned from elsewhere, and it is also not directly transferred from other expressions.
The second rule verifies that ``Expr`` has ``Val`` at ``Line``, and this value is not directly transferred from other expressions.
Note that ``val/3`` might not be recorded, hence it is possible that even if there exist other expressions which directly pass their values to ``Expr``, ``Expr`` is the first one observed with null value.


#### check_val(Expr, Val, Line).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.

*Rule definition*
```
check_val(Expr, Val, Line) :- has_val(Expr, Val, Line), !.
```

``check_val/3`` checks whether ``Expr`` has ``Val`` at ``Line``.
It can be used to simply verify ``Expr``'s value at a certain line.
If you need to list up candidate expressions having a certain value ``Val``, you need to use ``has_val/3`` instead.

#### has_val(Expr, Val, Line).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.

*Rule definition*
```
has_val(Expr, Val, Line) :- has_val_at(Expr, Val, Line) ; has_val(Expr, Val, Line, []).
has_val(Expr, Val, Line, Considered) :- 
    memberchk((Expr, Line), Considered),
    has_val_at(Expr, Val, Line).
has_val(Expr, Val, Line, Considered) :-     
    copied_from((Expr, Line), (Expr1, Line1)), 
    \+ memberchk((Expr1, Line1), Considered),
    has_val(Expr1, Val, Line1, [(Expr1, Line1)|Considered]).
has_val(Name, Val, _, Considered) :-
    is_var(Name),
    copied_from((Name, _), (Expr, Line1)),
    \+ memberchk((Expr, Line1), Considered),
    has_val(Expr, Val, Line1, [(Expr, Line1)|Considered]).
```

``has_val/3`` indicates that ``Expr`` has ``Val`` at ``Line``.
If ``Val`` and ``Line`` are given, it will list up all expressions ``Expr`` which may have ``Val`` at ``Line``.
This includes cases that ``Expr``'s value is not directly observed and thus ``val/3`` doesn't exist.
``has_val_at/3`` will check baseline cases, and other ``has_val/3`` predicates also consider cases that values are copied to ``Expr`` from others.

#### has_val_at(Expr, Val, Line).

*Parameters*
* ``Expr``: an expression.
* ``Val``: the value of ``Expr``.
* ``Line``: the code line where ``Expr`` exists.

*Rule definition*
```
has_val_at(Expr, Val, _) :- Expr == Val.
has_val_at(Expr, Val, _) :- literal(Expr, Val).
has_val_at(Expr, Val, Line) :- val(Expr, Val, Line).
has_val_at(Expr, Val, Line) :- assign(Expr, Val, Line).
has_val_at(Expr, Val, Line) :- val(Expr1, Val, Line), assign(Expr1, Expr, Line).
has_val_at(Expr, Val, Line) :- copied_from((Expr, Line), (Val, _)).
```

``has_val_at/3`` describes cases which ``Expr`` can have ``Val`` at ``Line``.
This includes that ``Expr`` itself is ``Val``, ``Expr`` is a literal with value ``Val``, ``Val`` or ``Expr1`` with ``Val`` is directly assigned to ``Expr``, and finally the value ``Val`` itself is copied to ``Expr``.

#### copied_from((Expr, Line), (Expr1, Line)).

*Parameters*
* ``Expr``: an expression.
* ``Line``: the code line where ``Expr`` exists.
* ``Expr1``: another expression which can transfer its value to ``Expr``.
* ``Line1``: the code line where ``Expr1`` exists.

*Rule definition*
```
copied_from((Expr, Line), (Expr1, Line)) :- assign(Expr, Expr1, Line).
copied_from((Expr, _), (Expr1, Line1)) :- param(Expr, N, M), method_invoc(MI, M, Line1), argument(Expr1, N, MI).
copied_from((Expr, Line), (Expr1, Line1)) :- method_invoc(Expr, M, Line), return(Expr1, M, Line1).
copied_from((Expr, _), (Expr1, Line1)) :- 
    param(Expr, N, M), 
    match_static_method(M, _, MI, Line1),
    argument(Expr1, N, MI).
copied_from((Expr, Line), (Expr1, Line1)) :- 
    match_static_method(M, _, Expr, Line),
    method_invoc(Expr, M, Line), 
    return(Expr1, M, Line1).
```

``copied_from/2`` describes cases that the value of ``Expr`` at ``Line`` can be copied another expression ``Expr1`` at ``Line1``.
The first three predicates indicate baseline cases that ``Expr1`` is assigned to ``Expr``, an actual parameter ``Expr1`` is passed to a formal parameter ``Expr`` by a method invocation ``MI``, and a method invocation ``Expr``'s value is copied from a returned expression ``Expr1`` of the callee ``M``.
The other two predicates add two special conditions for the second and third predicates using ``match_static_method/4``.
It is possible that the id of a method appeared in the source code is different from the actual method called during execution due to inheritance.
Using ``match_static_method/4``, static and dynamic methods are matched, so the transfer of a value can be correctly identified.

### Rules to Produce Additional Facts

#### name_ref(Name, Type).

*Parameters*
* ``Name``: the id of a name reference.
* ``Type``: the type of ``Name``.

*Rule definition*
```
name_ref(Name, Type) :- name_ref(Name, Type, _, _), !.
```

``name_ref/2`` can simplify ``name_ref/4``.

#### expr(Expr) / expr(Expr, Code).

*Parameters*
* ``Expr``: the id of an expression.
* ``Code``: the actual code fragment of ``Expr``.

*Rule definition*
```
expr(Expr) :- expr(Expr, _), !.
expr(Expr, Code) :- 
    expr(Expr, _, _, _, _, Code) ;
    expr(Expr, _, _, Code) ;
    name(Expr, _, _, _, _, Code) ;
    literal(Expr, _, _, _, _, Code), !.
```

``expr/1`` and ``expr/2`` can be used to identify facts which represent expressions, and simplify them.

#### literal(Literal, Val).

*Parameters*
* ``Literal``: the id of a literal.
* ``Val``: the actual value of ``Literal``.

*Rule definition*
```
literal(Literal, Val) :- literal(Literal, _, _, _, _, Val), !.
```

``literal/2`` simplifies ``literal/6``.

#### code(CodeId, Parent, Range).

*Parameters*
* ``CodeId``: the id of a code entity.
* ``Parent``: the id of the parent code entity.
* ``Range``: the code range of this code entity.

*Rule definition*
```
code(CodeId, Parent, Range) :- 
    code(CodeId, _, Parent, _, Range) ;
    block(CodeId, _, Parent, _, Range) ;
    stmt(CodeId, _, Parent, _, Range) ;
    expr(CodeId, _, Parent, _, Range, _) ;
    name(CodeId, _, Parent, _, Range, _) ;
    literal(CodeId, _, Parent, _, Range, _).
```

``code/3`` can be used to identify facts which represent code entities, and simplify them.

#### assign(Name, Expr1, Line).

*Parameters*
* ``Name``: a name.
* ``Expr1``: another expression which can transfer its value to ``Expr``.
* ``Line``: the code line where ``Expr`` exists.

*Rule definition*
```
assign(Name, Expr1, Line) :- is_var(Name), ref(Name, Expr, Line), assign(Expr, Expr1, Line).
```

This is a rule to add a semantic fact ``assign/3`` for cases which in fact a ``Name``'s value is changed inside an expression ``Expr``.
For instance, in ``this.field1 = method1()``, the assignment actually changes the value of ``field1``, not the expression ``this.field1``.
Hence this rule augments collected facts for such cases.


#### is_var(Name).

*Parameters*
* ``Name``: a name.

*Rule definition*
```
is_var(Name) :- name_ref(Name, var) ; name_ref(Name, param) ; name_ref(Name, field), !.
```

``is_var/1`` verifies whether the given ``Name`` is a variable.
In here, a variable indicates variables, parameters and fields.

#### find_method(Method, line(Class, LineNum)).

*Parameters*
* ``Method``: a method which contains the given code line.
* ``Class``: a class which ``Method`` belongs.
* ``LineNum``: a line number of the code line.

*Rule definition*
```
find_method(Method, line(Class, LineNum)) :-
    method(Method, range(Class, _, _, StartLine, EndLine)),
    StartLine =< LineNum, EndLine >= LineNum, !.
```

``find_method/2`` identifies a method contains the given code line.

#### called_method(Method, MethodName, Line).

*Parameters*
* ``Method``: a method which contains the given code line.
* ``Class``: a class which ``Method`` belongs.
* ``LineNum``: a line number of the code line.

*Rule definition*
```
called_method(Method, MethodName, Line) :-
    method_invoc(_, Method, Line),
    name_ref(Method, method, MethodName, _).
```

``called_method/3`` identifies the id of a method ``Method`` using the given ``MethodName`` which is called at ``Line``.

#### find_method_body(+Method, -Body).

*Parameters*
* ``Method``: a method which contains the given code line.
* ``Body``: the id of a code block which is the body of ``Method``.

*Rule definition*
```
find_method_body(Method, Body) :-
    find_method_decl(Method, MethodDecl),
    block(Body, block, MethodDecl, body, _).
```

``find_method_body/2`` finds a code block which is the body of a given ``Method``.

#### find_method_decl(Method, MethodDecl).

*Parameters*
* ``Method``: a method which contains the given code line.
* ``MethodDecl``: the id of a code entity which indicates the declaration of ``Method``.

*Rule definition*
```
find_method_decl(Method, MethodDecl) :-
    method(Method, Range),
    code(MethodDecl, method_declaration, _, _, Range).
```

``find_method_decl/2`` finds a code entity which is the declaration of a given ``Method``.

### Rules to Handle Stack Trace Information

#### match_static_method(Method, StaticMethod, Expr, Line).

*Parameters*
* ``Method``: a method which is actually called at runtime.
* ``StaticMethod``: a method appeared in source code.
* ``Expr``: the method invocation which invokes ``Method``/``StaticMethod``.
* ``Line``: the code line of ``Expr``.

*Rule definition*
```
match_static_method(Method, StaticMethod, Expr, Line) :- 
    trace(Trace, _, Method, _, Failure, _),
    trace(_, Trace, _, Line, Failure, _), %this is the line calling the Method.
    name_ref(Method, method, MethodName, _), !,
    name_ref(StaticMethod, method, MethodName, _),
    StaticMethod \== Method,
    method_invoc(Expr, StaticMethod, Line), !.
```

``match_static_method/4`` matches dynamic and static methods called by the same method invocation.
It uses the code line appeared in stack traces, and matches dynamically called one with the one appeared at the same code line.

#### trace_line(Trace, Line, Failure).

*Parameters*
* ``Trace``: the id of a stack trace.
* ``Line``: the code line ``line/2`` appeared in the stack trace.
* ``Failure``: the id of test failure ``test_failure/3`` which ``Trace`` belongs.

*Rule definition*
```
trace_line(Trace, Line, Failure) :- trace(Trace, _, _, Line, Failure, _).
```

``trace_line/3`` simplifies ``trace/6``.

#### is_non_target(Trace).

*Parameters*
* ``Trace``: the id of a stack trace.

*Rule definition*
```
is_non_target(Trace) :-
    trace(Trace, _, _, _, _, non_target).
```

``is_not_target/1`` checks whether ``Trace`` is a non-target trace. (i.e., a method not from the code base.)

#### target_trace(Trace, Callee, Line, Failure).

*Parameters*
* ``Trace``: the id of a stack trace.
* ``Callee``: the id of a stack trace called at ``Line`` of ``Trace``.
* ``Line``: the code line of ``Trace``.
* ``Failure``: the id of test failure ``test_failure/3`` which ``Trace`` belongs.

*Rule definition*
```
target_trace(Trace, Callee, Line, Failure) :- trace(Trace, Callee, _, Line, Failure, target).
```

``target_trace/4`` can be used to retrieve information of target stack traces.

#### from_test(line(ClassId, _)).

*Parameters*
* ``ClassId``: the id of a class.

*Rule definition*
```
from_test(line(ClassId, _)) :- class(ClassId, ClassName), is_test_class(ClassName), !.
```

``from_test/1`` checks whether a given line ``line/2`` is from a test.


#### is_test_class(ClassName).

*Parameters*
* ``ClassName``: a class name.

*Rule definition*
```
is_test_class(ClassName) :- test_failure(_, ClassName, _).
```

``is_test_class/1`` checks whether a given class name indicates a test class using test failure information.

### Common Utilities

#### remove_duplicates(Total, Unique).

*Parameters*
* ``Total``: a list of elements.
* ``Unique``: a list of unique elements.

*Rule definition*
```
remove_duplicates([], []).
remove_duplicates([H|T], [H|Unique]) :-
    remove_all(H, T, Removed),
    remove_duplicates(Removed, Unique).

remove_all(X, List, Result) :-
    exclude(==(X), List, Result).
```

``remove_duplicates/2`` removes duplicate elements from a given list.
If there exist duplicates, only the first element will remain in ``Unique``.
The original order of elements in ``Total`` will be preserved based on the first appearances in ``Unique``.
