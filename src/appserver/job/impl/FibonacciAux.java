
package appserver.job.impl;

public class FibonacciAux {

    Integer number = null;
    
    public FibonacciAux(Integer number) {
        this.number = number;
    }
    
    public Integer getResult() {
        return Fib(this.number);
    }
    //Naive implementation of Fibonacci
    public int Fib(Integer number){
        if(number <= 0){
            return 0;
        }
        if(number == 1 || number == 2){
            return 1;
        }
        return Fib(number-1)+Fib(number-2);
    }
}
