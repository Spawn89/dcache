// $Id$
// $Log: not supported by cvs2svn $
// Revision 1.8  2005/06/29 22:30:18  leoheska
// updates to support new srm functionality
//

/*
COPYRIGHT STATUS:
  Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
  software are sponsored by the U.S. Department of Energy under Contract No.
  DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
  non-exclusive, royalty-free license to publish or reproduce these documents
  and software for U.S. Government purposes.  All documents and software
  available from this server are protected under the U.S. and Foreign
  Copyright Laws, and FNAL reserves all rights.
 
 
 Distribution of the software available from this server is free of
 charge subject to the user following the terms of the Fermitools
 Software Legal Information.
 
 Redistribution and/or modification of the software shall be accompanied
 by the Fermitools Software Legal Information  (including the copyright
 notice).
 
 The user is asked to feed back problems, benefits, and/or suggestions
 about the software to the Fermilab Software Providers.
 
 
 Neither the name of Fermilab, the  URA, nor the names of the contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.
 
 
 
  DISCLAIMER OF LIABILITY (BSD):
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
  OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
  FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.
 
 
  Liabilities of the Government:
 
  This software is provided by URA, independent from its Prime Contract
  with the U.S. Department of Energy. URA is acting independently from
  the Government and in its own private capacity and is not acting on
  behalf of the U.S. Government, nor as its contractor nor its agent.
  Correspondingly, it is understood and agreed that the U.S. Government
  has no connection to this software and in no manner whatsoever shall
  be liable for nor assume any responsibility or obligation for any claim,
  cost, or damages arising out of or resulting from the use of the software
  available from this server.
 
 
  Export Control:
 
  All documents and software available from this server are subject to U.S.
  export control laws.  Anyone downloading information from this server is
  obligated to secure any necessary Government licenses before exporting
  documents or software obtained from this server.
 */


/*
 * @(#)ArgParser.java	0.9 05/27/02
 *
 * Copyright 2002 Fermi National Accelerator Lab. All rights reserved.
 * FNAL PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package gov.fnal.srm.util;

import org.dcache.srm.SRMException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A command line arguments parser,
 * ArgParser.java, Thr May 30 10:32:35 2002
 *
 * @author Timur Perelmutov
 * @author CD/ISD
 * @version 	0.9, 27 May 2002
 */

public class ArgParser {
   //
   // private constants
   //
   // tree types of options (-option1=4289 vs -option2=nfs vs -option3)
   
   private static final int INT_OPTION_TYPE = 0;
   private static final int STRING_OPTION_TYPE = 1;
   private static final int VOID_OPTION_TYPE = 2;
   private static final int BOOLEAN_OPTION_TYPE = 3;
   
   //
   // private fields
   //
   
   private Hashtable general_options = new Hashtable(); //these are parsed general options
   private Hashtable command_options = new Hashtable();         // and parsed  command options
   // typed by user in command line after
   // command
   private String[] argv;            // these are original arguments
   private Hashtable option_list =
      new Hashtable();          //the list of possible options
   private String command_name; // the command name
   //(first non optional argument)
   private Vector arguments=new Vector();// the vector of nonoptional arguments
   private String[] commands;
   
   private boolean parsed;
   private boolean usecommand;
   
   
   
   private  ArgParser() {
   }
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   
   public ArgParser(String[] argv,String[] commands) {
      this.argv = argv;
      if(commands != null && commands.length>0) {
         this.commands = commands;
         usecommand = true;
      }
   }
   
   public ArgParser(String[] argv) {
      this.argv = argv;
      usecommand = false;
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public void addIntegerOption(String command,
      String name,
      String description,
      int min_value,
      int max_value)
      throws SRMException {
      if(!usecommand && command != null) {
         throw new SRMException("attempt to specify option for " +
            "nonexistent command " + command);
      }
      if(parsed) {
         throw new SRMException("command line already parsed");
      }
      ArgOption option = new ArgOption();
      option.command = command;
      option.name = name;
      option.min_val = min_value;
      option.max_val = max_value;
      option.type = INT_OPTION_TYPE;
      option.description = description;
      if(command==null) {
         option_list.put(name,option);
      } else {
         //System.out.println("put("+name+",{"+option.command+","+option.name+"})");
         option_list.put(command+" "+name,option);
      }
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public void addStringOption(String command, String name, String description)
   throws SRMException {
      if(!usecommand && command != null) {
         throw new SRMException("attempt to specify option for " +
            "nonexistent command " + command);
      }
      if(parsed) {
         throw new SRMException("command line has been already parsed");
      }
      ArgOption option = new ArgOption();
      option.command = command;
      option.name = name;
      option.type = STRING_OPTION_TYPE;
      option.description = description;
      if(command==null) {
         option_list.put(name,option);
      } else {
         option_list.put(command+" "+name,option);
         //System.out.println("put("+name+",{"+option.command+","+option.name+"})");
      }
   }
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public void addBooleanOption(String command, String name, String description)
   throws SRMException {
      if(!usecommand && command != null) {
         throw new SRMException("attempt to specify option for " +
            "nonexistent command " + command);
      }
      if(parsed) {
         throw new SRMException("command line has been already parsed");
      }
      ArgOption option = new ArgOption();
      option.command = command;
      option.name = name;
      option.type = BOOLEAN_OPTION_TYPE;
      option.description = description;
      if(command==null) {
         option_list.put(name,option);
      } else {
         option_list.put(command+" "+name,option);
         //System.out.println("put("+name+",{"+option.command+","+option.name+"})");
      }
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public void addVoidOption(String command, String name, String description)
   throws SRMException {
      if(!usecommand && command != null) {
         throw new SRMException("attempt to specify option for " +
            "nonexistent command " + command);
      }
      if(parsed) {
         throw new SRMException("command line has been already parsed");
      }
      ArgOption option = new ArgOption();
      option.command = command;
      option.name = name;
      option.type = VOID_OPTION_TYPE;
      option.description = description;
      
      if(command==null) {
         option_list.put(name,option);
      } else {
         //System.out.println("put("+name+",{"+option.command+","+option.name+"})");
         option_list.put(command+" "+name,option);
      }
   }
   
   /**
    * Parses the command line and checks it validity </p>
    *
    * @throws SRMException
    *                when command line contains invalid
    *                option or invalid command or no command at all.
    *
    */
   
   public void parse() throws SRMException {
      if(parsed) {
         throw new SRMException(
            "command line has been already parsed");
      }
      String option_name;
      String option_value=null;
      boolean general_option = true;
      boolean two_dash_option =false;
      
      // for (int i=0;i<argv.length;++i) System.out.println(argv[i]);
      
      for (int i=0;i<argv.length;++i) {
         option_value=null;
         String next_arg = argv[i];
         if (next_arg.charAt(0) == '-' ) {
            next_arg=next_arg.substring(1);
            if(next_arg.length() == 0) {
               throw new SRMException("command line error, "+
                  "invalid option: \""+argv[i]+"\"");
            }
            if(next_arg.charAt(0) == '-' ) {
               next_arg=next_arg.substring(1);
               two_dash_option = true;
            }
            if(next_arg.length() == 0) {
               throw new SRMException("command line error, "+
                  "invalid option: \""+argv[i]+"\"");
            }
            int equals_index = next_arg.indexOf('=');
            if(equals_index == 0) {
               throw new SRMException("command line error, "+
                  "invalid option: "+argv[i]);
            }
            if(equals_index >0) {
               
               option_name = next_arg.substring(0,equals_index);
               option_value = next_arg.substring(equals_index+1);
            } else {
               option_name = next_arg;
            }
            
            // System.out.println("option_name: " + option_name);
            
            ArgOption option;
            
            //System.out.println("option name =  "+option_name);
            //System.out.println("option value =  "+option_value);
            
            if(general_option) {
               option = (ArgOption)option_list.get(option_name);
               // System.out.println(
               //    "getting option; key = " + option_name);
            } else {
               // System.out.println(
               //   "getting option; key = " + command_name + " " +
               //                               option_name);
               option = (ArgOption)option_list.get(
                  this.command_name + " " + option_name);
            }
            
            if(option == null) {
               //one more possibility, we are dealing with a negative number
               try {
                  Integer.parseInt(argv[i]);
                  this.arguments.add(argv[i]);
                  continue;
               } catch(NumberFormatException nfe) {
               }
               throw new SRMException("command line error, "+
                  "unrecognized option: "+argv[i]);
               
            }
            if(general_option && option.command != null) {
               throw new SRMException("command line error, "+
                  "misplaced command "+
                  option.command+
                  " option: "+argv[i]);
               
            }
            
            if(!general_option && option.command == null) {
               throw new SRMException("command line error, "+
                  "misplaced general option: "+argv[i]);
               
            }
            
            ArgOption new_option = option.copy();
            if(new_option.type == BOOLEAN_OPTION_TYPE) {
               if(option_value == null || option_value.equals(""))
                  option_value="true";
               
               new_option.bool_option_value = option_value.equalsIgnoreCase("true");
            } else if(new_option.type == STRING_OPTION_TYPE) {
               if(option_value == null || option_value.equals("")) {
                  if( (i+1) < argv.length) {
                     i++;
                     option_value = argv[i];
                  } else throw new SRMException(
                     "command line error, "+
                     "option value is not specified: "+argv[i]);
               }
               new_option.string_option_value = option_value;
            } else if(new_option.type == INT_OPTION_TYPE) {
               if(option_value == null || option_value.equals("")) {
                  if( (i+1) < argv.length || option_value.equals("")) {
                     i++;
                     option_value = argv[i];
                  } else {
                     throw new SRMException("command line error, "+
                        "option value is not specified: "+argv[i]);
                  }
               }
               
               try {
                  new_option.int_option_value = Integer.parseInt(option_value);
               } catch(NumberFormatException nfe) {
                  throw new SRMException("command line error, "+
                     "invalid option value: "+argv[i]+
                     "\n error:"+nfe.getMessage());
               }
               if(new_option.int_option_value < new_option.min_val ||
                  new_option.int_option_value > new_option.max_val) {
                  throw new SRMException("command line error, "+
                     "invalid option value: "+argv[i]+
                     "\n error: value is outside range ["+
                     new_option.min_val+","+new_option.max_val+"]");
               }
            }
            new_option.isDoubleDashOption = two_dash_option;
            if(general_option) {
               general_options.put(option_name,new_option);
            } else {
               command_options.put(command_name+" "+option_name,new_option);
            }
         }//if (argv[i].charAt(0) == '-')
         else {
            if(general_option && usecommand) {
               for(int j =0; j<this.commands.length;++j) {
                  if(argv[i].equals(commands[j])) {
                     this.command_name = argv[i];
                     break;
                  }
               }
               if(this.command_name == null) {
                  throw new SRMException("command line error, "+
                     "invalid command name: "+argv[i]);
               }
               general_option = false;
            } else {
               this.arguments.add(argv[i]);
            }
         }
      }
      if(this.command_name == null && usecommand ) {
         throw new SRMException("command line error, "+
            "command name not specified ");
      }
      parsed = true;
   } //public void parse()
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   
   public boolean isOptionSet( String command,String name) throws SRMException {
      if(!parsed) {
         throw new SRMException("command line hasn't been parsed yet");
      }
      
      if(command == null) {
         return general_options.containsKey(name);
      } else {
         return command_options.containsKey(command+" "+name);
      }
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   private ArgOption getOption(String command,String name) throws SRMException {
      if(!parsed) {
         throw new SRMException("command line not yet parsed");
      }
      ArgOption option;
      if(command == null) {
         option = (ArgOption) general_options.get(name);
         if(option == null) {
            throw new SRMException("option "+name+" is not set");
         }
      } else {
         option = (ArgOption) command_options.get(command+" "+name);
         if(option == null) {
            throw new SRMException("option "+name+" for command "+command+" is not set");
         }
      }
      return option;
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public boolean booleanOptionValue( String command, String name) throws SRMException {
      ArgOption option = getOption(command,name);
      if(option.type != BOOLEAN_OPTION_TYPE) {
         throw new SRMException("option is not a string option");
      }
      return option.bool_option_value;
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public String stringOptionValue( String command, String name) throws SRMException {
      ArgOption option = getOption(command,name);
      if(option.type == STRING_OPTION_TYPE) {
         return option.string_option_value;
      } else if(option.type == BOOLEAN_OPTION_TYPE) {
         return new Boolean(option.bool_option_value).toString();
      } else if(option.type == INT_OPTION_TYPE) {
         return new Integer(option.int_option_value).toString();
      } else if(option.type == VOID_OPTION_TYPE) {
         return "";
      } else {
         throw new SRMException("unknown option type");
      }
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public int intOptionValue(String command, String name) throws SRMException {
      ArgOption option = getOption(command,name);
      if(option.type != INT_OPTION_TYPE) {
         throw new SRMException("option is not a string option");
      }
      return option.int_option_value;
   }
   
   public String getCommand() throws SRMException {
      if(!parsed) {
         throw new SRMException("command line hasn't been parsed yet");
      }
      return command_name;
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public String[] getCommandArguments() throws SRMException {
      if(!parsed) {
         throw new SRMException("command line hasn't been parsed yet");
      }
      String[] rc = new String[arguments.size()];
      arguments.toArray(rc);
      return rc;
   }
   
   /**
    * Description </p>
    *
    * @param  param1
    *         param1 description
    * @param  param2
    *         param2 description
    * @return return value description
    *
    * @throws Exception
    *                when
    */
   public String usage() {
      
      StringBuffer sb = new StringBuffer();
      sb.append("Usage: srm [general-options] ");
      if(commands!=null) {
         sb.append("command [command-options-and-arguments]\n");
      }
      sb.append("  where general-options are:\n");
      Enumeration e = option_list.keys();
      while(e.hasMoreElements()) {
         String key = (String) e.nextElement();
         if(key.indexOf(' ')==-1) {
            ArgOption option = (ArgOption) option_list.get(key);
            optionToSB(sb,option);
         }
      }
      if(commands != null) {
         sb.append("  command :\n");
         
         for(int i = 0;i<commands.length;++i) {
            sb.append("    ").append(commands[i]).append('\n');
         }
         sb.append("--help-command <command-name> for command details");
      }
      return sb.toString();
   }
   
   public String usage(String command) {
      int i =0;
      for(;i<commands.length;++i) {
         if(commands[i].equals(command)) {
            break;
         }
      }
      if(i == commands.length) {
         return "unknown command "+command+ ", srm --help for help";
      }
      
      StringBuffer sb = new StringBuffer();
      sb.append(command);
      sb.append(" usage: srm [general-options] "+ command +" [options-and-arguments]\n");
      sb.append("  where options-and-arguments are:\n");
      Enumeration e = option_list.keys();
      while (e.hasMoreElements()) {
         String key = (String) e.nextElement();
         if (key.indexOf(' ') != -1  &&
            key.startsWith(command+' ') ) {
            ArgOption option = (ArgOption)option_list.get(key);
            if(option == null) {
               // shoul never happen
               //throw new SRMException("option for the key \""+key +"\" is null");
            }
            optionToSB(sb,option);
         }
      }
      return sb.toString();
   }
   
   private static void optionToSB(StringBuffer sb, ArgOption option) {
      sb.append("      -").append(option.name);
      if(option.type == INT_OPTION_TYPE) {
         sb.append("=<int-value> where ").append(option.min_val);
         sb.append(
            " is less than <int-value>, and <int-value> is less than ").
               append(option.max_val);
      } else if(option.type == STRING_OPTION_TYPE) {
         sb.append("=<string-value>");
      }
      
      if(option.description != null &&
         !option.description.equalsIgnoreCase("description")) {
         sb.append("\n\t\t").append(option.description);
      }
      sb.append('\n');
   }
   
   /**
    * A command line argument,
    * ArgParser.ArgOption
    *
    */
   private class ArgOption {
      String command;
      String name;
      int type;
      int min_val,max_val;
      String string_option_value;
      int int_option_value;
      boolean bool_option_value;
      String description;
      boolean isDoubleDashOption;
      
      protected Object clone() {
         ArgOption the_clone;
         try {
            the_clone = (ArgOption) super.clone();
         } catch(CloneNotSupportedException cnse) {
            the_clone = new ArgOption();
         }
         the_clone.command = this.command;
         the_clone.name = this.name;
         the_clone.type = this.type;
         the_clone.min_val = this.min_val;
         the_clone.max_val = this.max_val;
         the_clone.string_option_value=this.string_option_value;
         the_clone.int_option_value = int_option_value;
         the_clone.description = this.description;
         return the_clone;
      }
      
      ArgOption copy() {
         return (ArgOption) clone();
      }
   }
   
}