package neoe.aspect;

import org.aspectj.lang.annotation.*;
import org.aspectj.lang.JoinPoint;
import java.awt.print.Printable; // Import needed for return type check

/**
 * Aspect to verify state transitions AND simple sequential properties
 * in Print.java during manual execution.
 * Combines original sequential checks with MBT state tracking.
 */
@Aspect
public class PrintTestAspect {

    // --- Original Sequential Test Flags ---
    private boolean drawStringCallFlag = false; // Renamed slightly to avoid clash
    private boolean drawReturnCallFlag = false; // Renamed slightly
    private boolean getTotalPagesCallFlag = false; // Renamed slightly
    private boolean printCallFlag = false;       // Renamed slightly
    private boolean printPagesCallFlag = false;    // Renamed slightly

    // --- State Tracking based on MBT Model ---
    private enum PrintState { IDLE, INITIALIZING, SHOWING_PAGE_DIALOG, CALCULATING, READY_TO_PRINT, SHOWING_PRINT_DIALOG, PRINTING, PAGE_RENDERED, COMPLETE, CANCELLED, ERROR, NO_PAGES }
    private PrintState currentState = PrintState.IDLE;
    private int pagesCalculated = -1;
    private int currentPageIndex = -1;


    // --- Pointcuts (Defining where to intercept) ---
    // Using named pointcuts for better readability and reuse

    @Pointcut("execution(* neoe.ne.Print.printPages(..))")
    void printPagesExecution() {}

    @Pointcut("execution(* neoe.ne.Print.getTotalPage(..))")
    void getTotalPageExecution() {}

    @Pointcut("execution(* neoe.ne.Print.print(..))")
    void printExecution() {}

    @Pointcut("execution(* neoe.ne.Print.drawStringLine(..))")
    void drawStringLineExecution() {}

    @Pointcut("execution(* neoe.ne.Print.drawReturn(..))")
    void drawReturnExecution() {}


    // --- Advice for State Tracking AND Original Flag Setting ---

    // Reset state and flags when printPages starts
    @Before("printPagesExecution()")
    public void beforePrintPages(JoinPoint jp) {
        System.out.println("\n--- Aspect: Entering printPages() ---");
        // State Tracking Reset
        currentState = PrintState.INITIALIZING;
        pagesCalculated = -1;
        currentPageIndex = -1;
        System.out.println("Aspect State: -> INITIALIZING");

        // Original Flags Reset & Set
        printPagesCallFlag = true; // Set this flag now
        getTotalPagesCallFlag = false;
        printCallFlag = false;
        drawStringCallFlag = false;
        drawReturnCallFlag = false;
        System.out.println("Aspect Flags: printPages=true, others=false");
    }

    @Before("getTotalPageExecution()")
    public void beforeGetTotalPage(JoinPoint jp) {
        System.out.println("Aspect: Entering getTotalPage(). Current State: " + currentState);
        // State Tracking
        if (currentState != PrintState.INITIALIZING && currentState != PrintState.SHOWING_PAGE_DIALOG) {
            System.err.println("❌ State Warning: getTotalPage called in unexpected state: " + currentState);
        }
        currentState = PrintState.CALCULATING;
        System.out.println("Aspect State: -> CALCULATING");

        // Original Flag
        this.getTotalPagesCallFlag = true;
        System.out.println("Aspect Flag: getTotalPages=true");
        System.out.println("Tracked Method: " + jp.getSignature().getName()); // Original output
    }

    @AfterReturning(pointcut="getTotalPageExecution()", returning="retVal")
    public void afterReturningTotalPage(JoinPoint jp, Object retVal) {
        System.out.println("Aspect: Exiting getTotalPage(). Current State: " + currentState);
        // State Tracking
        if (retVal instanceof Integer) {
            pagesCalculated = (Integer) retVal;
            System.out.println("Aspect: Pages calculated = " + pagesCalculated);
            if (pagesCalculated > 0) {
                currentState = PrintState.READY_TO_PRINT;
                System.out.println("Aspect State: -> READY_TO_PRINT (Pages > 0)");
            } else {
                currentState = PrintState.NO_PAGES;
                System.out.println("Aspect State: -> NO_PAGES (Pages <= 0)");
            }
        } else {
            System.err.println("❌ State Error: getTotalPage returned non-Integer: " + retVal);
            currentState = PrintState.ERROR;
            System.out.println("Aspect State: -> ERROR");
        }
        // Original Check Call
        verifyTotalPageOrder();
    }


    @Before("printExecution() && args(graphics, pageFormat, pageIndex)")
    public void beforePrint(JoinPoint jp, Object graphics, Object pageFormat, int pageIndex) {
        currentPageIndex = pageIndex;
        System.out.println("Aspect: Entering print() for page " + pageIndex + ". Current State: " + currentState);

        // State Tracking Checks
        if (currentState != PrintState.READY_TO_PRINT && currentState != PrintState.PRINTING && currentState != PrintState.PAGE_RENDERED ) {
            System.err.println("❌ State Warning: print() called in unexpected state: " + currentState);
        }
        if (!getTotalPagesCallFlag || pagesCalculated <= 0) { // Use original flag here too for consistency
            System.err.println("❌ State Error: print() called before totalPages calculated or when pages <= 0!");
            currentState = PrintState.ERROR;
        } else if (pageIndex >= pagesCalculated) {
            System.err.println("❌ State Error: print() called for pageIndex (" + pageIndex + ") >= totalPages (" + pagesCalculated + ")");
            currentState = PrintState.ERROR;
        } else {
            currentState = PrintState.PRINTING;
            System.out.println("Aspect State: -> PRINTING (Page " + pageIndex + ")");
        }

        // Original Flag
        this.printCallFlag = true;
        System.out.println("Aspect Flag: printCall=true");
        System.out.println("Tracked Method: " + jp.getSignature().getName()); // Original output
    }


    @AfterReturning(pointcut="printExecution() && args(graphics, pageFormat, pageIndex)", returning="retVal")
    public void afterReturningPrint(JoinPoint jp, Object graphics, Object pageFormat, int pageIndex, Object retVal) {
        System.out.println("Aspect: Exiting print() for page " + pageIndex + ". Return: " + retVal + ". Current State: " + currentState);
        // State Tracking
        if (currentState != PrintState.ERROR) { // Don't update state if error already detected
            if (retVal instanceof Integer) {
                int result = (Integer) retVal;
                if (result == Printable.PAGE_EXISTS) {
                    currentState = PrintState.PAGE_RENDERED;
                    System.out.println("Aspect State: -> PAGE_RENDERED (Page " + pageIndex + ")");
                    if (pageIndex == pagesCalculated - 1) {
                        System.out.println("Aspect: Last expected page rendered.");
                        // Cannot reliably set COMPLETE here, depends on external PrinterJob state
                    }
                } else if (result == Printable.NO_SUCH_PAGE) {
                    System.err.println("❌ State Error: print() returned NO_SUCH_PAGE unexpectedly for page " + pageIndex);
                    currentState = PrintState.ERROR;
                    System.out.println("Aspect State: -> ERROR");
                }
            }
        }
        // Original Check Call
        verifyPrintOrder();
    }

    // Track drawStringLine and drawReturn calls for original flags
    @After("drawStringLineExecution()")
    public void trackDrawStringExecution(JoinPoint joinPoint) {
        System.out.println("Aspect: drawStringLine executed. Current State: " + currentState);
        if (currentState != PrintState.PRINTING) {
            System.err.println("❌ State Warning: drawStringLine called outside of PRINTING state: " + currentState);
        }
        this.drawStringCallFlag = true;
        System.out.println("Aspect Flag: drawStringCall=true");
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName()); // Original output
        // Original Check Call
        verifyDrawStringOrder();
    }

    @Before("drawReturnExecution()") // Changed to @Before to match original logic intent
    public void trackDrawReturnExecution(JoinPoint joinPoint) {
        System.out.println("Aspect: Entering drawReturn. Current State: " + currentState);
        if (currentState != PrintState.PRINTING) {
            System.err.println("❌ State Warning: drawReturn called outside of PRINTING state: " + currentState);
        }
        this.drawReturnCallFlag = true; // Set flag *before* verifyDrawReturnOrder is called by drawReturn's @After
        System.out.println("Aspect Flag: drawReturnCall=true");
        System.out.println("Tracked Method: " + joinPoint.getSignature().getName()); // Original output
    }

    @After("drawReturnExecution()") // Added @After for drawReturn to call the original verification method
    public void afterDrawReturn(JoinPoint jp) {
        System.out.println("Aspect: Exiting drawReturn.");
        // Original Check Call
        verifyDrawReturnOrder();
        // Reset flag? Depends if drawReturn should always follow drawStringLine *immediately*
        // this.drawReturnCallFlag = false; // Optional reset
    }


    // Exception Handling
    @AfterThrowing(pointcut="execution(* neoe.ne.Print.*(..))", throwing="ex")
    public void afterThrowingException(JoinPoint jp, Throwable ex) {
        System.err.println("❌ Aspect Exception: Exception thrown from " + jp.getSignature().getName() + ": " + ex);
        currentState = PrintState.ERROR;
        System.out.println("Aspect State: -> ERROR");
    }

    // Optional: Check state at the end of printPages
    @After("printPagesExecution()")
    public void afterPrintPages(JoinPoint jp) {
        System.out.println("--- Aspect: Exiting printPages() ---");
        System.out.println("Aspect Final State (observed): " + currentState);
        // Final state checks... (same as before)
    }


    // --- Original Sequential Verification Methods ---
    // These are now called from the appropriate @AfterReturning or @After advice above

    // @After("execution(* neoe.ne.Print.getTotalPage(..))") // No longer needed as separate advice
    public void verifyTotalPageOrder() {
        // Uses original flag: printPagesCallFlag
        if (!this.printPagesCallFlag) {
            System.err.println("\n❌ Sequential Check: getTotalPages execution order is incorrect! (printPages not called first)");
        } else {
            System.out.println("\n✅ Sequential Check: getTotalPages execution appears correct.");
        }
    }

    // @After("execution(* neoe.ne.Print.print(..))") // No longer needed as separate advice
    public void verifyPrintOrder() {
        // Uses original flag: getTotalPagesCallFlag
        if (!this.getTotalPagesCallFlag) {
            System.err.println("\n❌ Sequential Check: print execution order is incorrect! (getTotalPage not called first)");
        } else {
            System.out.println("\n✅ Sequential Check: print execution appears correct.");
        }
    }

    // @After("execution(* neoe.ne.Print.drawStringLine(..))") // No longer needed as separate advice
    public void verifyDrawStringOrder() {
        // Uses original flag: printCallFlag
        if (!this.printCallFlag) {
            System.err.println("\n❌ Sequential Check: drawStringLine execution order is incorrect! (print not called first)");
        } else {
            System.out.println("\n✅ Sequential Check: drawStringLine execution appears correct.");
        }
    }

    // @After("execution(* neoe.ne.Print.drawReturn(..))") // No longer needed as separate advice
    public void verifyDrawReturnOrder() {
        // Uses original flag: drawStringCallFlag
        // Note: This assumes drawReturn *always* follows drawStringLine. If drawStringLine
        // can be skipped for empty lines but drawReturn still happens, this check might fail.
        if (!this.drawStringCallFlag) {
            System.err.println("\n❌ Sequential Check: drawReturn execution order is incorrect! (drawStringLine not called first)");
        } else {
            System.out.println("\n✅ Sequential Check: drawReturn execution appears correct.");
        }
        // Reset drawStringCallFlag after check? If multiple drawString/drawReturn pairs per print() call.
        // this.drawStringCallFlag = false; // Optional reset
    }
}