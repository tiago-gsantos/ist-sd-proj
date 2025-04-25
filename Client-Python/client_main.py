import sys
from typing import List
from client_service import ClientService
from command_processor import CommandProcessor

class ClientMain:
    @staticmethod
    def main(args: List[str]):
        print("Python Client")

        debug = False

        # check arguments
        # check if debug mode is enabled
        if len(args) == 3 and args[2] == "-debug":
            debug = True
            print("\n# Client in debug mode\n", file=sys.stderr)

            # print arguments
            print(f"# Received {len(args)} arguments", file=sys.stderr)
            for i, arg in enumerate(args):
                print(f"# arg[{i}] = {arg}", file=sys.stderr)
            print()

        elif len(args) != 2:
            print("Argument(s) missing!", file=sys.stderr)
            print("Usage: python3 client_main.py <host:port> <client_id> [-debug]", file=sys.stderr)
            return

        # get the host and port of the server or front-end
        host_port = args[0]

        # get the client id
        try:
            client_id = int(args[1])
        except ValueError:
            print("Invalid client id! Must be an integer.", file=sys.stderr)
            print("Usage: python3 client_main.py <host:port> <client_id> [-debug]", file=sys.stderr)
            return

        parser = CommandProcessor(ClientService(host_port, client_id, debug))
        parser.parse_input()


if __name__ == "__main__":
    ClientMain.main(sys.argv[1:])