nd PolicyAction. These condition and action objects are tied to instances of PolicyRule by the PolicyConditionInPolicyRule and PolicyActionInPolicyRule aggregations. 

A PolicyRule may also be associated with one or more policy time periods, indicating the schedule according to which the policy rule is active and inactive. In this case it is the PolicySetValidityPeriod aggregation that provides this linkage. 

The PolicyRule class uses the property ConditionListType, to indicate whether the conditions for the rule are in DNF (disjunctive normal form), CNF (conjunctive normal form) or, in the case of a rule with no conditions, as an UnconditionalRule. The PolicyConditionInPolicyRule aggregation contains two additional properties to complete the representation of the Rule\'s conditional expression. The first of these properties is an integer to partition the referenced PolicyConditions into one or more groups, and the second is a Boolean to indicate whether a referenced Condition is negated. An example shows how ConditionListType and these two additional properties provide a unique representation of a set of PolicyConditions in either DNF or CNF. 

Suppose we have a PolicyRule that aggregates five PolicyConditions C1 through C5, with the following values in the properties of the five PolicyConditionInPolicyRule associations: 
C1: GroupNumber = 1, ConditionNegated = FALSE 
C2: GroupNumber = 1, ConditionNegated = TRUE 
C3: GroupNumber = 1, ConditionNegated = FALSE 
C4: GroupNumber = 2, ConditionNegated = FALSE 
C5: GroupNumber = 2, ConditionNegated = FALSE 

If ConditionListType = DNF, then the overall condition for the PolicyRule is: 
(C1 AND (NOT C2) AND C3) OR (C4 AND C5) 

On the other hand, if ConditionListType = CNF, then the overall condition for the PolicyRule is: 
(C1 OR (NOT C2) OR C3) AND (C4 OR C5) 

In both cases, there is an unambiguous specification of the overall condition that is tested to determine whether to perform the PolicyActions associated with the PolicyRule. 

PolicyRule instances may also be used to aggregate other PolicyRules and/or PolicyGroups. When used in this way to implement nested rules, the conditions of the aggregating rule apply to the subordinate rules as well. However, any side effects of condition evaluation or the execution of actions MUST NOT affect the result of the evaluation of other conditions evaluated by the rule engine in the same evaluation pass. That is, an implementation of a rule engine MAY evaluate all conditions in any order before applying the priority and determining which actions are to be executed.