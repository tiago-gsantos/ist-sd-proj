import sys
from typing import List

import grpc
import time

class CommandProcessor:
    SPACE = " "
    BGN_TUPLE = "<"
    END_TUPLE = ">"
    PUT = "put"
    READ = "read"
    TAKE = "take"
    SLEEP = "sleep"
    EXIT = "exit"
    GET_TUPLE_SPACES_STATE = "getTupleSpacesState"

    def __init__(self, client_service):
        self.client_service = client_service

    def parse_input(self):
        exit_flag = False
        while not exit_flag:
            try:
                line = input("> ").strip()
                split = line.split(self.SPACE)
                command = split[0]

                if command == self.PUT:
                    self.put(split)
                elif command == self.READ:
                    self.read(split)
                elif command == self.TAKE:
                    self.take(split)
                elif command == self.GET_TUPLE_SPACES_STATE:
                    self.get_tuple_spaces_state()
                elif command == self.SLEEP:
                    self.sleep(split)
                elif command == self.EXIT:
                    exit_flag = True
                else:
                    self.print_usage()
            except EOFError:
                break
        self.client_service.close()


    def put(self, split: List[str]):
        # check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]

        try:
            self.client_service.put(tuple_data)
            print("OK\n")
        except grpc.RpcError as e:
            # print error description
            print("\nERROR\n" + e.details() + "\n", file=sys.stderr)

    def read(self, split: List[str]):
        # check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]

        try:
            response = self.client_service.read(tuple_data)
            print("OK\n" + response + "\n")
        except grpc.RpcError as e:
            # print error description
            print("\nERROR\n" + e.details() + "\n", file=sys.stderr)

    def take(self, split: List[str]):
        # check if the input is valid
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]

        try:
            response = self.client_service.take(tuple_data)
            print("OK\n" + response + "\n")
        
        except grpc.RpcError as e:
            # print error description
            print("\nERROR\n" + e.details() + "\n", file=sys.stderr)
        

    def get_tuple_spaces_state(self):
        try:
            response = self.client_service.getTupleSpacesState()
            print("OK\n" + response + "\n")

        except grpc.RpcError as e:
            # print error description
            print("\nERROR\n" + e.details() + "\n", file=sys.stderr)

    def sleep(self, split: List[str]):
        if len(split) != 2:
            self.print_usage()
            return
        
        try:
            time_to_sleep = int(split[1])
        except ValueError:
            self.print_usage()
            return

        try:
            time.sleep(time_to_sleep)
        except KeyboardInterrupt as e:
            raise RuntimeError("Sleep interrupted") from e
        
    def print_usage(self):
        print("Usage:\n"
              "- put <element[,more_elements]>\n"
              "- read <element[,more_elements]>\n"
              "- take <element[,more_elements]>\n"
              "- getTupleSpacesState\n"
              "- sleep <integer>\n"
              "- exit\n")

    def input_is_valid(self, input_data: List[str]) -> bool:
        if (len(input_data) < 2
                or not input_data[1].startswith(self.BGN_TUPLE)
                or not input_data[1].endswith(self.END_TUPLE)
                or len(input_data) > 2):
            return False
        return True