using System.IO;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace Company.FunctionApp
{
    public static class Function
    {
        [FunctionName("Function")]
        public static void Main(string[] args)
        {
            Console.WriteLine("Hello World!");
        }
    }
}
