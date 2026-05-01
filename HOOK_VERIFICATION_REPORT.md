Hook Signature Verification Report
===================================

Target Skript Version: 2.14.2

1. registerComparator (org.skriptlang.skript.lang.comparator.Comparators)
   JAR Signature: public static <T1, T2> void registerComparator(Class<T1> firstType, Class<T2> secondType, Comparator<T1, T2> comparator)
   Hook Target: registerComparator
   Expected Arguments: 3 (firstType: Class<?>, secondType: Class<?>, comparator: Comparator<?, ?>)
   Hook Implementation: RegisterComparatorAdvice 
     - Uses @Advice.Argument(0), (1), (2)
     - Specific typed parameters
   Status: ✓ CORRECT - Single method, 3 args only

2. registerDifference (org.skriptlang.skript.lang.arithmetic.Arithmetics)
   JAR Signature(s):
     a) public static <T> void registerDifference(Class<T> type, Operation<T, T, T> operation)
        Arguments: 2 (type: Class<T>, operation: Operation<T, T, T>)
     b) public static <T, R> void registerDifference(Class<T> type, Class<R> returnType, Operation<T, T, R> operation)
        Arguments: 3 (type: Class<T>, returnType: Class<R>, operation: Operation<T, T, R>)
   Hook Target: registerDifference
   Hook Implementation: RegisterDifferenceAdvice
     - Uses @Advice.AllArguments
     - Collector checks: args.length == 2 OR args.length == 3
   Status: ✓ CORRECT - Handles both 2-arg and 3-arg overloads

3. registerConverter (org.skriptlang.skript.lang.converter.Converters)
   JAR Signature(s):
     a) public static <F, T> void registerConverter(Class<F> fromType, Class<T> toType, Converter<F, T> converter)
        Arguments: 3 (fromType: Class<F>, toType: Class<T>, converter: Converter<F, T>)
     b) public static <F, T> void registerConverter(Class<F> fromType, Class<T> toType, Converter<F, T> converter, int flags)
        Arguments: 4 (fromType: Class<F>, toType: Class<T>, converter: Converter<F, T>, flags: int)
   Hook Target: registerConverter
   Hook Implementation: RegisterConverterAdvice
     - Uses @Advice.AllArguments
     - Collector checks: args.length == 3 OR args.length == 4
   Status: ✓ CORRECT - Handles both 3-arg and 4-arg overloads

4. registerOperation (org.skriptlang.skript.lang.arithmetic.Arithmetics)
   JAR Signature(s):
     a) public static <T> void registerOperation(Operator operator, Class<T> type, Operation<T, T, T> operation)
        Arguments: 3 (operator, type, operation)
     b) public static <L, R> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Operation<L, R, L> operation)
        Arguments: 4 (operator, leftClass, rightClass, operation)
     c) public static <L, R> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Operation<L, R, L> operation, Operation<R, L, L> commutativeOperation)
        Arguments: 5 (operator, leftClass, rightClass, operation, commutativeOperation)
     d) public static <L, R, T> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType, Operation<L, R, T> operation, Operation<R, L, T> commutativeOperation)
        Arguments: 6 (operator, leftClass, rightClass, returnType, operation, commutativeOperation)
     e) public static <L, R, T> void registerOperation(Operator operator, Class<L> leftClass, Class<R> rightClass, Class<T> returnType, Operation<L, R, T> operation)
        Arguments: 5 (operator, leftClass, rightClass, returnType, operation)
   Hook Target: registerOperation
   Hook Implementation: RegisterOperationAdvice
     - Uses @Advice.AllArguments
     - Collector checks: args.length == 5 AND specific pattern match
   Status: ⚠ ISSUE - Only hooking the 5-arg variant (args.length == 5)
   Recommendation: The 5-arg leaf method is sufficient since other overloads call it, BUT verify in logs

5. Operator.registerOperator (for Operator registration tracking)
   Current Hook: RegisterOperatorHook targets registerOperation (same as #4)
   Status: ⚠ POTENTIAL ISSUE - Operator is a record/class, not a method target
   Recommendation: If tracking Operator constructor is needed, separate hook required

SUMMARY:
- registerComparator: ✓ Correct
- registerDifference: ✓ Correct (handles 2 and 3 args)
- registerConverter: ✓ Correct (handles 3 and 4 args)
- registerOperation: ⚠ Partial (5-arg variant only, but should be sufficient as it's the leaf)
- Operator tracking: ⚠ May need review if constructor tracking is required

RECOMMENDATION:
Hook all registerOperation overloads to ensure no registration is missed:
- Add isValidArguments checks for 3-arg, 4-arg, 5-arg, 6-arg patterns
- Normalize all to extract: operator, left, right, returnType (with defaults)

