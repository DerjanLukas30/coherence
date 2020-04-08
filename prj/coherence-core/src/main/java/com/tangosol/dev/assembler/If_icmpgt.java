/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* The IF_ICMPGT op branches to the label if the second integer on the stack
* is greater than the first integer on the stack.
* <p><code><pre>
* JASM op         :  IF_ICMPGT    (0xa3)
* JVM byte code(s):  IF_ICMPGT    (0xa3)
* Details         :
* </pre></code>
*
* @version 0.50, 06/14/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class If_icmpgt extends OpBranch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the label to branch to
    */
    public If_icmpgt(Label label)
        {
        super(IF_ICMPGT, label);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "If_icmpgt";
    }
