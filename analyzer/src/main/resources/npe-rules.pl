:- style_check(-discontiguous).

:- multifile assign/3.
:- multifile class/2.
:- multifile method/2.
:- multifile expr/4.
:- multifile expr/6.
:- multifile name/6.
:- multifile literal/6.
:- multifile name_ref/4.
:- multifile code/3.
:- multifile code/5.
:- multifile block/5.
:- multifile stmt/5.
:- multifile test_failure/3.
:- multifile trace/6.

:- dynamic ref/3.
:- dynamic val/3.
:- dynamic argument/3.
:- dynamic method_invoc/3.
:- dynamic return/3.
:- dynamic param/3.
:- dynamic throw/3.
:- dynamic throw/2.
:- dynamic log_msg/1.

% *** Rules for identifying NPE causes. ***
find_npe_cause(Expr, Line, Cause, Loc) :- 
    findall((Expr, Line, Cause, Loc), (npe(Expr, Line), cause_of(npe(Expr, Line), Cause, Loc)), TotalCauses),
    remove_duplicates(TotalCauses, UniqueCauses),
    rank_causes(UniqueCauses, RankedCauses),
    member((Expr, Line, Cause, Loc), RankedCauses).

%Rules to idenitfy the null expression.
npe(Expr, Line) :- candidate_line(Line), find_null_expr(Expr, Line).

%Identify candidate lines using trace information.
candidate_line(Line) :- test_failure(Failure, _, _),
    first_target_trace(_, Line, Failure).
first_target_trace(Trace, Line, Failure) :- 
    target_trace(Trace, Failure, Line, Failure), 
    \+throw_at_line(Line).
first_target_trace(Trace, Line, Failure) :- 
    target_trace(Trace, Next, Line, Failure),
    Next \== Failure,
    (is_non_target(Next) ; 
    	trace_line(Next, NextLine, Failure),
    	throw_at_line(NextLine)), !.
throw_at_line(line(ClassId, LineNum)) :- stmt(_, throw_statement, _, _, range(ClassId, _, _, LineNum, _)).

find_null_expr(Expr, Line) :- 
    findall((Expr, Line), null_arg_passed(Expr, Line) ; null_ref(Expr, Line), Candidates),
    remove_duplicates(Candidates, NullExprs),
    member((Expr, Line), NullExprs).
null_arg_passed(Expr, Line) :-
    trace_called_method(_, M, Line),
    find_null_arg(Expr, Line, _, M).
%Expr is a null argument of the invocation MI, calling the method M at Line.
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

null_ref(Expr, Line) :- 
    ref(Expr, _, Line), check_val(Expr, null, Line).

%Rules to identify the cause and location from the given NPE.
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

%Rules to rank identified causes and locations.
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

is_null_return(Method, Cause) :-
    return(Cause, Method, Line),
    return(Expr, Method, Line1),
    Cause \== Expr, Line \== Line1,
    literal(Cause, null),
    \+ literal(Expr, null).
val_assigned_in_method(Expr, Line, Method) :- 
    assign(Expr, _, Line1), find_method(Method, Line1), 
    Line = line(_, LineNum), Line1 = line(_, LineNum1),
    LineNum >= LineNum1.
only_target_method(Method) :- 
    trace(Trace, Callee, Method, _, Failure, target),    
    trace(Callee, _, _, _, Failure, non_target),
    trace(_, Trace, _, Line, Failure, target),
    from_test(Line).
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

%Rules for common behaviour.
originated_from(val(Expr, Val, Line), (Origin, Loc)) :-    
    (not_from_others(Expr, Val, Line), ! ; val_first_introduced(Expr, Val, Line)),
    Origin = Expr, Loc = Line.
originated_from(val(Expr, Val, Line), (Origin, Loc)) :-    
    can_be_transferred(val(Expr, Val, Line), (Expr1, Line1)),
    (not_from_others(Expr1, null, Line1), ! ; val_first_introduced(Expr1, Val, Line1)),
    Origin = Expr1, Loc = Line1.

%This part keeps looking for another expr which transfers the value to the current expr.
can_be_transferred(val(Expr, Val, Line), (Expr1, Line1)) :-
    findall((Expr1, Line1), can_be_transferred_internal(val(Expr, Val, Line), (Expr1, Line1)), Pairs),
    remove_duplicates(Pairs, UniquePairs),
    member((Expr1, Line1), UniquePairs).
can_be_transferred_internal(val(Expr, Val, Line), (Expr1, Line1)) :-
	single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)) ;
    multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), [(Expr, Line)]).

single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)) :-
    copied_from_expr(val(Expr, Val, Line), (Expr1, Line1)).
single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)) :-
    is_var(Expr),
    assigned_to_var(val(Expr, Val, Line), (Expr1, Line1)).
single_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered) :-
    single_step_transfer(val(Expr, Val, Line), (Expr1, Line1)),
    \+ memberchk((Expr1, Line1), Considered).

multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered) :-
    single_step_transfer(val(Expr, Val, Line), (Expr2, Line2), Considered),
    single_step_transfer(val(Expr2, Val, Line2), (Expr1, Line1), [(Expr2, Line2)|Considered]).
multi_step_transfer(val(Expr, Val, Line), (Expr1, Line1), Considered) :-
    single_step_transfer(val(Expr, Val, Line), (Expr2, Line2), Considered),
    multi_step_transfer(val(Expr2, Val, Line2), (Expr1, Line1), [(Expr2, Line2)|Considered]).

%copied from another expression.
copied_from_expr(val(Expr, Val, Line), (Expr1, Line1)) :-
    find_candidates((Expr1, Line1), copied_from((Expr, Line), (Expr1, Line1))),
    Expr1 \== Expr,
    check_val(Expr1, Val, Line1).

%assigned to a name.
assigned_to_var(val(Name, Val, Line), (Expr1, Line1)) :-
    check_val(Name, Val, Line),
    assign(Name, Expr1, Line1),
    check_val(Expr1, Val, Line1).

%Val is not copied from elsewhere.
not_from_others(Expr, Val, Line) :-
    check_val(Expr, Val, Line),
    \+ ((single_step_transfer(val(Expr, Val, Line), (_, Line1)), \+ from_test(Line1))).
%Val is first introduced to Expr.
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

%check_val/3 is only used to verify Expr has Val at Line.
check_val(Expr, Val, Line) :- has_val(Expr, Val, Line), !.

%has_val/3 can produce candidates.
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

%has_val_at/3 provides base cases.
has_val_at(Expr, Val, _) :- Expr == Val.
has_val_at(Expr, Val, _) :- literal(Expr, Val).
has_val_at(Expr, Val, Line) :- val(Expr, Val, Line).
has_val_at(Expr, Val, Line) :- assign(Expr, Val, Line).
has_val_at(Expr, Val, Line) :- val(Expr1, Val, Line), assign(Expr1, Expr, Line).
has_val_at(Expr, Val, Line) :- copied_from((Expr, Line), (Val, _)).

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

%Rules for analyze and extract info. from collected facts
name_ref(Name, Type) :- name_ref(Name, Type, _, _), !.
expr(Expr) :- expr(Expr, _), !.
expr(Expr, Code) :- 
    expr(Expr, _, _, _, _, Code) ;
    expr(Expr, _, _, Code) ;
    name(Expr, _, _, _, _, Code) ;
    literal(Expr, _, _, _, _, Code), !.
literal(Literal, Val) :- literal(Literal, _, _, _, _, Val), !.
code(CodeId, Parent, Range) :- 
    code(CodeId, _, Parent, _, Range) ;
    block(CodeId, _, Parent, _, Range) ;
    stmt(CodeId, _, Parent, _, Range) ;
    expr(CodeId, _, Parent, _, Range, _) ;
    name(CodeId, _, Parent, _, Range, _) ;
    literal(CodeId, _, Parent, _, Range, _).

assign(Name, Expr1, Line) :- is_var(Name), ref(Name, Expr, Line), assign(Expr, Expr1, Line).
is_var(Name) :- name_ref(Name, var) ; name_ref(Name, param) ; name_ref(Name, field), !.
find_method(Method, line(Class, LineNum)) :-
    method(Method, range(Class, _, _, StartLine, EndLine)),
    StartLine =< LineNum, EndLine >= LineNum, !.
called_method(Method, MethodName, Line) :-
    method_invoc(_, Method, Line),
    name_ref(Method, method, MethodName, _).
find_method_body(Method, Body) :-
    find_method_decl(Method, MethodDecl),
    block(Body, block, MethodDecl, body, _).
find_method_decl(Method, MethodDecl) :-
    method(Method, Range),
    code(MethodDecl, method_declaration, _, _, Range).

%Handling stack trace information.
match_static_method(Method, StaticMethod, Expr, Line) :- 
    trace(Trace, _, Method, _, Failure, _),
    trace(_, Trace, _, Line, Failure, _), %this is the line calling the Method.
    name_ref(Method, method, MethodName, _), !,
    name_ref(StaticMethod, method, MethodName, _),
    StaticMethod \== Method,
    method_invoc(Expr, StaticMethod, Line), !.
trace_line(Trace, Line, Failure) :-
    trace(Trace, _, _, Line, Failure, _).
is_non_target(Trace) :-
    trace(Trace, _, _, _, _, non_target).
target_trace(Trace, Callee, Line, Failure) :-
    trace(Trace, Callee, _, Line, Failure, target).
from_test(line(ClassId, _)) :-
    class(ClassId, ClassName), is_test_class(ClassName), !.
is_test_class(ClassName) :-
    test_failure(_, ClassName, _).


%Common utilities for convenience.
remove_duplicates([], []).
remove_duplicates([H|T], [H|Unique]) :-
    remove_all(H, T, Removed),
    remove_duplicates(Removed, Unique).

remove_all(X, List, Result) :-
    exclude(==(X), List, Result).

find_candidates((Expr, Line), Pred) :-
    findall((Expr, Line), Pred, Exprs),
    remove_duplicates(Exprs, Unique),
    member((Expr, Line), Unique).
    

logging(Message) :- format(atom(M), '~w', [Message]), assertz(log_msg(M)).
logging(Message, Arg) :- format(atom(M), Message, Arg), assertz(log_msg(M)).

pop_log(Messages) :-
    findall(M, log_msg(M), Messages),
    retractall(log_msg(_)).

%Generate simple, direct messages for FL results.
explain((X, L, Cause, Code, Loc), Explanation) :-
    format(atom(Explanation), 'NPE(~q is null at ~q) might be caused by ~q (~q) at ~q.', [X, L, Cause, Code, Loc]).
explain([], []).
explain([(X, L, Cause, Code, Loc)|RestTuples], [Explanation|RestExplanations]) :-
    explain((X, L, Cause, Code, Loc), Explanation),
    explain(RestTuples, RestExplanations).    