package com.bridgelabz.parkinglot.service;
import com.bridgelabz.parkinglot.enums.DriverType;
import com.bridgelabz.parkinglot.enums.VehicleColor;
import com.bridgelabz.parkinglot.enums.VehicleSize;
import com.bridgelabz.parkinglot.exception.ParkingLotException;
import com.bridgelabz.parkinglot.exception.ParkingLotException.ExceptionType;
import com.bridgelabz.parkinglot.model.ParkingSlotDetails;
import com.bridgelabz.parkinglot.observer.AirportSecurityImpl;
import com.bridgelabz.parkinglot.observer.ParkingLotObserver;
import com.bridgelabz.parkinglot.observer.ParkingOwnerImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
public class ParkingLot {
    Map<Integer, ParkingSlotDetails> parkingLotData = new HashMap<>();
    public final int TOTAL_PARKING_LOT_CAPACITY;
    private final int TOTAL_LOTS;
    private final int SINGLE_LOT_CAPACITY;
    public static boolean LatestVehicleStatus;
    private int lotNo = 1;
    ParkingLotObserver parkingLotObserver = new ParkingLotObserver();

    public ParkingLot(int parkingLotCapacity, int noOfLots) {
        this.TOTAL_LOTS = noOfLots;
        this.TOTAL_PARKING_LOT_CAPACITY = parkingLotCapacity * noOfLots;
        this.SINGLE_LOT_CAPACITY = parkingLotCapacity;
        ParkingOwnerImpl parkingOwner = new ParkingOwnerImpl();
        AirportSecurityImpl airportSecurity = new AirportSecurityImpl();
        parkingLotObserver.registerForStatus(parkingOwner);
        parkingLotObserver.registerForStatus(airportSecurity);
    }

    public void park(ParkingSlotDetails vehicle) throws ParkingLotException {
        if (this.isPresent(vehicle)) {
            throw new ParkingLotException(ExceptionType.VEHICLE_ALREADY_PARKED, "This vehicle already parked");
        }
        if (parkingLotData.size() == TOTAL_PARKING_LOT_CAPACITY) {
            parkingLotObserver.notificationUpdate(true);
            throw new ParkingLotException(ExceptionType.PARKING_LOT_IS_FULL, "Parking lot is full");
        }
        this.parkingLot(vehicle);
        vehicleStatus(true);
    }

    public void unPark(String vehicle) throws ParkingLotException {
        isMyVehiclePresent(vehicle);
        int counter = 0;
        for (ParkingSlotDetails t : parkingLotData.values()) {
            counter++;
            if (t.vehicleNumber.equals(vehicle)) {
                parkingLotData.remove(counter);
                parkingLotObserver.notificationUpdate(false);
                vehicleStatus(false);
                break;
            }
        }
    }

    private void vehicleStatus(boolean status) {
        ParkingLot.LatestVehicleStatus = status;
    }

    public int allocateAvailableLot(ParkingSlotDetails vehicle) throws ParkingLotException {
        if (this.isPresent(vehicle)) {
            throw new ParkingLotException(ExceptionType.VEHICLE_ALREADY_PARKED, "This vehicle already parked");
        }
        int lotStarted;
        int lotEnded;
        switch (vehicle.driverType) {
            case HANDICAP_DRIVER:
                lotStarted = 1;
                lotEnded = TOTAL_PARKING_LOT_CAPACITY;
                break;
            default:
                lotStarted = (SINGLE_LOT_CAPACITY * lotNo) - (SINGLE_LOT_CAPACITY - 1);
                lotEnded = SINGLE_LOT_CAPACITY * lotNo;
                break;
        }
        for (int slotNo = lotStarted; slotNo <= lotEnded; slotNo++) {
            if (vehicle.vehicleSize == VehicleSize.LARGE) {
                if (!parkingLotData.containsKey(slotNo) && !parkingLotData.containsKey(slotNo + 1)) {
                    return slotNo;
                }
            } else if (!parkingLotData.containsKey(slotNo)) {
                return slotNo;
            }
        }
        lotNo++;
        return this.allocateAvailableLot(vehicle);
    }

    public void parkingLot(ParkingSlotDetails vehicle) throws ParkingLotException {
        int slotNo = allocateAvailableLot(vehicle);
        ParkingOwnerImpl.lotNoForCar(lotNo);
        parkingLotData.putIfAbsent(slotNo, vehicle);
        if (vehicle.driverType == DriverType.NON_HANDICAP_DRIVER) {
            lotNo++;
        }
        if (lotNo == TOTAL_LOTS +1) {
            lotNo = 1;
        }
    }

    public boolean isMyVehiclePresent(String vehicleNumber) throws ParkingLotException {
        for (ParkingSlotDetails ParkingSlotDetails : parkingLotData.values()) {
            if (ParkingSlotDetails.vehicleNumber.equals(vehicleNumber)) {
                return true;
            }
        }
        throw new ParkingLotException(ExceptionType.VEHICLE_NOT_PARKED, "Vehicle Not present");
    }

    private boolean isPresent(ParkingSlotDetails vehicle) {
        return parkingLotData.values().stream().anyMatch(ParkingSlotDetails -> ParkingSlotDetails.vehicleNumber.equals(vehicle.vehicleNumber));
    }

    public LocalDateTime vehicleArrivedTime(String givenCarName) throws ParkingLotException {
        for (ParkingSlotDetails ParkingSlotDetails : parkingLotData.values()) {
            if (ParkingSlotDetails.vehicleNumber.equals(givenCarName)) {
                return ParkingSlotDetails.vehicleParkingTime;
            }
        }
        throw new ParkingLotException(ParkingLotException.ExceptionType.VEHICLE_NOT_PARKED, "Vehicle Not present");
    }

    public List<Integer> FindVehicleLocationsByColor(VehicleColor color) {
        return parkingLotData.entrySet().stream().filter(entry -> Objects.equals(entry.getValue().vehicleColor,
                color)).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}


